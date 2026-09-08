package com.rupp.trafficvision;

import com.rupp.trafficvision.model.ProcessingParameters;
import com.rupp.trafficvision.processing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ImageProcessingTest {

    private BufferedImage testImage;
    private ProcessingParameters params;

    @BeforeEach
    public void setup() {
        testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(new Color(255, 0, 0)); // Red block
        g.fillRect(0, 0, 50, 100);
        g.setColor(new Color(0, 255, 0)); // Green block
        g.fillRect(50, 0, 50, 100);
        g.dispose();

        params = new ProcessingParameters();
    }

    @Test
    public void testGrayscaleProcessor() {
        GrayscaleProcessor processor = new GrayscaleProcessor();
        BufferedImage gray = processor.process(testImage, params);

        assertNotNull(gray);
        assertEquals(100, gray.getWidth());
        assertEquals(100, gray.getHeight());

        // Verify R=G=B in grayscale output
        int rgb = gray.getRGB(25, 50);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        assertEquals(r, g);
        assertEquals(g, b);
    }

    @Test
    public void testEdgeDetectionProcessor() {
        EdgeDetectionProcessor processor = new EdgeDetectionProcessor();
        BufferedImage edges = processor.process(testImage, params);

        assertNotNull(edges);
        assertEquals(100, edges.getWidth());
        assertEquals(100, edges.getHeight());

        // Boundary between red and green at x=50 should have prominent edge magnitude
        int edgeRgb = edges.getRGB(50, 50) & 0xFF;
        assertTrue(edgeRgb > 50, "Vertical boundary should exhibit strong gradient edge");
    }

    @Test
    public void testBlurProcessor() {
        BlurProcessor processor = new BlurProcessor();
        BufferedImage blurred = processor.process(testImage, params);

        assertNotNull(blurred);
        assertEquals(100, blurred.getWidth());
        assertEquals(100, blurred.getHeight());
    }

    @Test
    public void testThresholdProcessor() {
        ThresholdProcessor processor = new ThresholdProcessor();
        params.setThreshold(100);
        BufferedImage thresh = processor.process(testImage, params);

        assertNotNull(thresh);
        int rgb = thresh.getRGB(25, 50) & 0xFF;
        assertTrue(rgb == 0 || rgb == 255, "Binary threshold pixel must be 0 or 255");
    }

    @Test
    public void testContrastProcessor() {
        ContrastProcessor processor = new ContrastProcessor();
        params.setContrast(1.5);
        params.setBrightness(20.0);
        BufferedImage contrasted = processor.process(testImage, params);

        assertNotNull(contrasted);
        assertEquals(100, contrasted.getWidth());
    }
}
