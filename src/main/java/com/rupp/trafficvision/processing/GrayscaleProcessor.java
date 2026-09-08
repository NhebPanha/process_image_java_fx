package com.rupp.trafficvision.processing;

import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Converts a color image to grayscale using ITU-R BT.601 luminance weighting:
 * Y = 0.299*R + 0.587*G + 0.114*B.
 */
public class GrayscaleProcessor implements ImageProcessor {

    @Override
    public BufferedImage process(BufferedImage input, ProcessingParameters params) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                gray = Math.max(0, Math.min(255, gray));

                int grayRgb = (gray << 16) | (gray << 8) | gray;
                output.setRGB(x, y, grayRgb);
            }
        }
        return output;
    }
}
