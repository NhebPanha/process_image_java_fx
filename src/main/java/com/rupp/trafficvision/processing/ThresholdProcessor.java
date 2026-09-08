package com.rupp.trafficvision.processing;

import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Applies binary thresholding to segment pixels above or below a luminance cutoff.
 */
public class ThresholdProcessor implements ImageProcessor {

    @Override
    public BufferedImage process(BufferedImage input, ProcessingParameters params) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();
        int threshold = params != null ? params.getThreshold() : 128;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int binary = gray >= threshold ? 255 : 0;
                int binaryRgb = (binary << 16) | (binary << 8) | binary;
                output.setRGB(x, y, binaryRgb);
            }
        }
        return output;
    }
}
