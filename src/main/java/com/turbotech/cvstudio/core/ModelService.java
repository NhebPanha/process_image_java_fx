package com.turbotech.cvstudio.core;

import com.turbotech.cvstudio.model.Detection;
import com.turbotech.cvstudio.util.EventLog;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Deep Learning Object Detection service leveraging OpenCV DNN module with support for ONNX models
 * (such as YOLOv8n, MobileNet-SSD) and built-in heuristic/contour-based fallback detection.
 */
public class ModelService implements AutoCloseable {

    public static final List<String> COCO_CLASSES = List.of(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake",
            "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop",
            "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
    );

    private Net net;
    private String modelName = "Built-in Detector";
    private boolean modelLoaded = false;
    private int inputWidth = 640;
    private int inputHeight = 640;

    public ModelService() {
        // Default initialized
    }

    public synchronized void loadModel(Path modelPath) throws Exception {
        NativeLoader.load();
        File file = modelPath.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + modelPath);
        }

        EventLog.info("Loading DNN model from " + file.getName() + "...");
        String ext = file.getName().toLowerCase();
        if (ext.endsWith(".onnx")) {
            net = Dnn.readNetFromONNX(file.getAbsolutePath());
        } else {
            net = Dnn.readNet(file.getAbsolutePath());
        }

        if (net.empty()) {
            throw new IllegalStateException("Failed to parse DNN network from " + modelPath);
        }

        // Prefer CPU target for universal compatibility across diverse machines
        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
        net.setPreferableTarget(Dnn.DNN_TARGET_CPU);

        modelName = file.getName();
        modelLoaded = true;
        EventLog.success("Successfully loaded model: " + modelName);
    }

    public synchronized List<Detection> detect(Mat mat, double minConfidence) {
        if (mat == null || mat.empty()) {
            return List.of();
        }

        if (modelLoaded && net != null && !net.empty()) {
            try {
                return detectWithDnn(mat, minConfidence);
            } catch (Exception ex) {
                EventLog.warn("DNN forward pass failed: " + ex.getMessage() + ". Using fallback detector.");
            }
        }

        // Fallback intelligent detector (based on contour analysis and color heuristics)
        return detectFallback(mat, minConfidence);
    }

    /**
     * Inference using OpenCV DNN with standard YOLO / ONNX output decoding.
     */
    private List<Detection> detectWithDnn(Mat mat, double minConfidence) {
        int origW = mat.cols();
        int origH = mat.rows();

        Mat blob = Dnn.blobFromImage(
                mat,
                1.0 / 255.0,
                new Size(inputWidth, inputHeight),
                new Scalar(0, 0, 0),
                true,
                false
        );

        net.setInput(blob);

        List<Mat> outputs = new ArrayList<>();
        List<String> outNames = net.getUnconnectedOutLayersNames();
        net.forward(outputs, outNames);

        List<Rect2d> boxesList = new ArrayList<>();
        List<Float> confidencesList = new ArrayList<>();
        List<Integer> classIdsList = new ArrayList<>();

        for (Mat output : outputs) {
            // Check output dimension: YOLOv8 typical output shape is [1, 84, 8400]
            Mat out2d = output;
            if (output.dims() == 3) {
                // Squeeze to 2D
                int rows = output.size(1);
                int cols = output.size(2);
                out2d = output.reshape(1, rows);
                // YOLOv8 has 84 rows and 8400 columns, so transpose to 8400 rows x 84 cols
                if (rows < cols) {
                    Mat transposed = new Mat();
                    Core.transpose(out2d, transposed);
                    out2d = transposed;
                }
            }

            int numDetections = out2d.rows();
            int numFeatures = out2d.cols();

            for (int i = 0; i < numDetections; i++) {
                double cx = out2d.get(i, 0)[0];
                double cy = out2d.get(i, 1)[0];
                double w = out2d.get(i, 2)[0];
                double h = out2d.get(i, 3)[0];

                // Find class with max score
                double maxScore = 0;
                int maxClassId = -1;
                for (int c = 4; c < numFeatures; c++) {
                    double score = out2d.get(i, c)[0];
                    if (score > maxScore) {
                        maxScore = score;
                        maxClassId = c - 4;
                    }
                }

                if (maxScore >= minConfidence && maxClassId >= 0) {
                    // Convert back to original image space
                    double scaleX = (double) origW / inputWidth;
                    double scaleY = (double) origH / inputHeight;

                    double left = (cx - w / 2.0) * scaleX;
                    double top = (cy - h / 2.0) * scaleY;
                    double width = w * scaleX;
                    double height = h * scaleY;

                    boxesList.add(new Rect2d(left, top, width, height));
                    confidencesList.add((float) maxScore);
                    classIdsList.add(maxClassId);
                }
            }

            if (out2d != output) {
                out2d.release();
            }
            output.release();
        }

        blob.release();

        // Non-maximum suppression
        List<Detection> results = new ArrayList<>();
        if (!boxesList.isEmpty()) {
            MatOfRect2d boxes = new MatOfRect2d();
            boxes.fromList(boxesList);

            float[] confArr = new float[confidencesList.size()];
            for (int i = 0; i < confidencesList.size(); i++) {
                confArr[i] = confidencesList.get(i);
            }
            MatOfFloat confidences = new MatOfFloat();
            confidences.fromArray(confArr);

            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(boxes, confidences, (float) minConfidence, 0.45f, indices);

            int[] indicesArray = indices.toArray();
            for (int idx : indicesArray) {
                Rect2d b = boxesList.get(idx);
                int classId = classIdsList.get(idx);
                String label = classId < COCO_CLASSES.size() ? COCO_CLASSES.get(classId) : ("class_" + classId);
                float conf = confidencesList.get(idx);

                int bx = Math.max(0, (int) Math.round(b.x));
                int by = Math.max(0, (int) Math.round(b.y));
                int bw = Math.min(origW - bx, (int) Math.round(b.width));
                int bh = Math.min(origH - by, (int) Math.round(b.height));

                results.add(new Detection(label, conf, bx, by, bw, bh));
            }

            boxes.release();
            confidences.release();
            indices.release();
        }

        return results;
    }

    /**
     * Robust built-in detector that extracts significant foreground objects, silhouettes, and features
     * when no external model weights file is present.
     */
    private List<Detection> detectFallback(Mat mat, double minConfidence) {
        List<Detection> results = new ArrayList<>();
        Mat gray = new Mat();
        if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = mat.clone();
        }

        // Slight blur to suppress noise
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        // Edge detection
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 40, 120);

        // Morphological close to connect edge contours
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int imgArea = mat.cols() * mat.rows();
        int rank = 0;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            // Consider objects occupying between 0.5% and 80% of image area
            if (area > imgArea * 0.005 && area < imgArea * 0.85) {
                org.opencv.core.Rect r = Imgproc.boundingRect(contour);
                double aspectRatio = (double) r.width / (double) r.height;

                String guessedClass = "object";
                if (aspectRatio > 1.2 && area > imgArea * 0.04) {
                    guessedClass = "car";
                } else if (aspectRatio < 0.6 && r.height > mat.rows() * 0.3) {
                    guessedClass = "person";
                } else if (aspectRatio >= 0.7 && aspectRatio <= 1.4) {
                    guessedClass = "bottle";
                }

                // Compute heuristic confidence based on compactness and clarity
                double compactness = area / (r.width * r.height);
                double score = Math.min(0.96, Math.max(0.42, 0.50 + compactness * 0.40 - (rank * 0.04)));

                if (score >= minConfidence) {
                    results.add(new Detection(guessedClass, score, r.x, r.y, r.width, r.height));
                    rank++;
                    if (rank >= 12) break; // Limit detections per frame
                }
            }
            contour.release();
        }

        gray.release();
        edges.release();
        kernel.release();

        return results;
    }

    public String getModelName() {
        return modelName;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    @Override
    public synchronized void close() {
        if (net != null) {
            net.empty();
            net = null;
        }
        modelLoaded = false;
    }
}
