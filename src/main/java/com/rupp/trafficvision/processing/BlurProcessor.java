package com.rupp.trafficvision.processing;

import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Applies a Gaussian-weighted smoothing convolution kernel for noise attenuation.
 */
public class BlurProcessor implements ImageProcessor {

    private static final int[][] GAUSSIAN_KERNEL = {
            {1, 2, 1},
            {2, 4, 2},
            {1, 2, 1}
    };
    private static final int KERNEL_SUM = 16;

    @Override
    public BufferedImage process(BufferedImage input, ProcessingParameters params) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    output.setRGB(x, y, input.getRGB(x, y));
                    continue;
                }

                int rSum = 0;
                int gSum = 0;
                int bSum = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = input.getRGB(x + kx, y + ky);
                        int weight = GAUSSIAN_KERNEL[ky + 1][kx + 1];
                        rSum += ((rgb >> 16) & 0xFF) * weight;
                        gSum += ((rgb >> 8) & 0xFF) * weight;
                        bSum += (rgb & 0xFF) * weight;
                    }
                }

                int r = rSum / KERNEL_SUM;
                int g = gSum / KERNEL_SUM;
                int b = bSum / KERNEL_SUM;

                output.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return output;
    }
}
