package com.nhebpanha.cvstudio.pipeline;

import com.nhebpanha.cvstudio.core.ImageUtils;
import com.nhebpanha.cvstudio.model.FeatureSet;
import com.nhebpanha.cvstudio.model.Frame;
import com.nhebpanha.cvstudio.model.PipelineSettings;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 3: Feature Extraction.
 * Identifies key patterns, edges, contours, textures, or keypoints in the image.
 */
public class FeatureExtractionStage implements PipelineStage {

    private ORB orb;

    public FeatureExtractionStage() {
        // Lazily initialized upon first use to ensure native libraries are loaded
    }

    private synchronized ORB getOrb() {
        if (orb == null) {
            orb = ORB.create(500);
        }
        return orb;
    }

    @Override
    public String name() {
        return "3 Feature Extraction";
    }

    @Override
    public Frame apply(Frame frame, PipelineSettings s) {
        Mat src = frame.getProcessed();
        if (src == null || src.empty()) {
            src = frame.getOriginal();
        }
        if (src == null || src.empty()) {
            return frame;
        }

        Mat gray = new Mat();
        if (src.channels() == 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        } else if (src.channels() == 4) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY);
        } else {
            src.copyTo(gray);
        }

        FeatureSet fs = new FeatureSet();
        Mat view = new Mat();
        Imgproc.cvtColor(gray, view, Imgproc.COLOR_GRAY2BGR);

        PipelineSettings.FeatureMode mode = s.getFeatureMode();
        if (mode == null) mode = PipelineSettings.FeatureMode.EDGES;

        switch (mode) {
            case EDGES -> {
                Mat edges = new Mat();
                Imgproc.Canny(gray, edges, s.getCannyLow(), s.getCannyHigh());
                fs.setEdges(edges.clone());
                Imgproc.cvtColor(edges, view, Imgproc.COLOR_GRAY2BGR);
                edges.release();
            }
            case CONTOURS -> {
                Mat edges = new Mat();
                Imgproc.Canny(gray, edges, s.getCannyLow(), s.getCannyHigh());
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(edges, contours, hierarchy,
                        Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                contours.removeIf(c -> Imgproc.contourArea(c) < s.getMinContourArea());
                Imgproc.drawContours(view, contours, -1, new Scalar(0, 255, 120), 2);
                fs.setContourCount(contours.size());

                for (MatOfPoint c : contours) {
                    c.release();
                }
                hierarchy.release();
                edges.release();
            }
            case KEYPOINTS -> {
                MatOfKeyPoint kp = new MatOfKeyPoint();
                Mat descriptors = new Mat();
                Mat mask = new Mat();
                try {
                    getOrb().detectAndCompute(gray, mask, kp, descriptors);
                    Features2d.drawKeypoints(view, kp, view, new Scalar(0, 200, 255), 0);
                    fs.setKeypoints(kp);
                    fs.setDescriptors(descriptors);
                } catch (Exception ex) {
                    kp.release();
                    descriptors.release();
                } finally {
                    mask.release();
                }
            }
            case HISTOGRAM -> {
                Mat hist = ImageUtils.computeHistogram(gray);
                Mat histView = ImageUtils.renderHistogram(hist, Math.max(320, src.cols()), Math.max(240, src.rows()));
                view.release();
                view = histView;
                hist.release();
            }
        }

        // Clean up previous feature view
        if (frame.getFeatureView() != null) {
            frame.getFeatureView().release();
        }
        if (frame.getFeatures() != null) {
            frame.getFeatures().release();
        }

        frame.setFeatures(fs);
        frame.setFeatureView(view);
        gray.release();

        return frame;
    }
}
