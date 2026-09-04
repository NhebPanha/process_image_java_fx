package com.turbotech.cvstudio;

import com.turbotech.cvstudio.core.ImageUtils;
import com.turbotech.cvstudio.core.NativeLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ImageUtilsTest {

    @BeforeAll
    static void initOpenCV() {
        NativeLoader.load();
    }

    @Test
    void testToBufferedImage() {
        Mat mat = new Mat(120, 160, CvType.CV_8UC3, new Scalar(100, 150, 200));
        BufferedImage img = ImageUtils.toBufferedImage(mat);

        assertNotNull(img);
        assertEquals(160, img.getWidth());
        assertEquals(120, img.getHeight());

        mat.release();
    }

    @Test
    void testHistogram() {
        Mat gray = new Mat(100, 100, CvType.CV_8UC1, new Scalar(128));
        Mat hist = ImageUtils.computeHistogram(gray);

        assertNotNull(hist);
        assertEquals(256, hist.total());

        Mat histView = ImageUtils.renderHistogram(hist, 320, 200);
        assertNotNull(histView);
        assertEquals(320, histView.cols());
        assertEquals(200, histView.rows());

        gray.release();
        hist.release();
        histView.release();
    }
}
