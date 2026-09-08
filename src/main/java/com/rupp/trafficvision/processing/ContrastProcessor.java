package com.rupp.trafficvision.processing;

import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Adjusts image contrast and brightness according to parametric controls.
 */
public class ContrastProcessor implements ImageProcessor {

    @Override
    public BufferedImage process(BufferedImage input, ProcessingParameters params) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();

        double contrast = params != null ? params.getContrast() : 1.0;
        double brightness = params != null ? params.getBrightness() : 0.0;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int nr = clamp((int) Math.round((r - 128) * contrast + 128 + brightness));
                int ng = clamp((int) Math.round((g - 128) * contrast + 128 + brightness));
                int nb = clamp((int) Math.round((b - 128) * contrast + 128 + brightness));

                output.setRGB(x, y, (nr << 16) | (ng << 8) | nb);
            }
        }
        return output;
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
