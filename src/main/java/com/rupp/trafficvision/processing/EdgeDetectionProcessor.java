package com.rupp.trafficvision.processing;

import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Detects edges within an image using the 3x3 Sobel spatial gradient operator.
 * Highlights vehicle outlines, contours, wheels, and structural edges.
 */
public class EdgeDetectionProcessor implements ImageProcessor {

    private static final int[][] SOBEL_X = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };

    private static final int[][] SOBEL_Y = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
    };

    @Override
    public BufferedImage process(BufferedImage input, ProcessingParameters params) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();

        // Convert to grayscale first
        int[][] gray = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y][x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Compute Sobel convolution
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = 0;
                int gy = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = gray[y + ky][x + kx];
                        gx += pixel * SOBEL_X[ky + 1][kx + 1];
                        gy += pixel * SOBEL_Y[ky + 1][kx + 1];
                    }
                }

                int magnitude = (int) Math.min(255, Math.hypot(gx, gy));
                int edgeRgb = (magnitude << 16) | (magnitude << 8) | magnitude;
                output.setRGB(x, y, edgeRgb);
            }
        }
        return output;
    }
}
