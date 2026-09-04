package com.nhebpanha.cvstudio.core;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance utilities for converting between OpenCV Mat and JavaFX Image representations,
 * as well as image file I/O.
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Fast path Mat to JavaFX WritableImage conversion reusing existing buffers where possible.
     */
    public static WritableImage toFxImage(Mat mat, WritableImage reuse) {
        if (mat == null || mat.empty() || mat.cols() <= 0 || mat.rows() <= 0) {
            return null;
        }

        Mat rgb = new Mat();
        if (mat.channels() == 1) {
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_GRAY2RGB);
        } else if (mat.channels() == 4) {
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGRA2RGB);
        } else {
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);
        }

        int w = rgb.cols();
        int h = rgb.rows();
        byte[] buffer = new byte[w * h * 3];
        rgb.get(0, 0, buffer);

        WritableImage img = (reuse != null && (int) reuse.getWidth() == w && (int) reuse.getHeight() == h)
                ? reuse
                : new WritableImage(w, h);

        img.getPixelWriter().setPixels(
                0, 0, w, h,
                PixelFormat.getByteRgbInstance(),
                buffer, 0, w * 3
        );

        rgb.release();

        return img;
    }

    /**
     * Converts OpenCV Mat to AWT BufferedImage.
     */
    public static BufferedImage toBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }

        int type = (mat.channels() == 1) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return image;
    }

    /**
     * Computes a 256-bin grayscale or single channel histogram.
     */
    public static Mat computeHistogram(Mat gray) {
        List<Mat> images = new ArrayList<>();
        images.add(gray);
        MatOfInt channels = new MatOfInt(0);
        Mat mask = new Mat();
        Mat hist = new Mat();
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);

        Imgproc.calcHist(images, channels, mask, hist, histSize, ranges);
        mask.release();
        channels.release();
        histSize.release();
        ranges.release();

        return hist;
    }

    /**
     * Renders a 256-bin histogram Mat into a visualization Mat.
     */
    public static Mat renderHistogram(Mat hist, int width, int height) {
        Mat canvas = new Mat(height, width, CvType.CV_8UC3, new Scalar(25, 27, 34));

        Core.normalize(hist, hist, 0, canvas.rows() - 20, Core.NORM_MINMAX);
        float[] histData = new float[(int) hist.total()];
        hist.get(0, 0, histData);

        int binWidth = Math.max(1, (int) Math.round((double) width / histData.length));
        for (int i = 1; i < histData.length; i++) {
            Point p1 = new Point(binWidth * (i - 1), height - Math.round(histData[i - 1]) - 10);
            Point p2 = new Point(binWidth * i, height - Math.round(histData[i]) - 10);
            Imgproc.line(canvas, p1, p2, new Scalar(0, 215, 255), 2);
        }

        // Draw baseline
        Imgproc.line(canvas, new Point(0, height - 10), new Point(width, height - 10), new Scalar(80, 80, 90), 1);

        return canvas;
    }

    /**
     * Saves a Mat to a file on disk.
     */
    public static boolean saveMatImage(Mat mat, File file) {
        if (mat == null || mat.empty() || file == null) {
            return false;
        }
        return Imgcodecs.imwrite(file.getAbsolutePath(), mat);
    }
}
