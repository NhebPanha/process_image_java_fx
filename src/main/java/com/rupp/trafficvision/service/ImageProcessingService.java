package com.rupp.trafficvision.service;

import com.rupp.trafficvision.model.ProcessingParameters;
import com.rupp.trafficvision.processing.*;

import java.awt.image.BufferedImage;

/**
 * Service coordinating single and multi-stage image processing transformations.
 */
public class ImageProcessingService {

    private final GrayscaleProcessor grayscaleProcessor = new GrayscaleProcessor();
    private final EdgeDetectionProcessor edgeProcessor = new EdgeDetectionProcessor();
    private final BlurProcessor blurProcessor = new BlurProcessor();
    private final ThresholdProcessor thresholdProcessor = new ThresholdProcessor();
    private final ContrastProcessor contrastProcessor = new ContrastProcessor();

    /**
     * Executes image transformation according to selected processing mode and active parameters.
     *
     * @param input  source image
     * @param params processing parameters and mode
     * @return transformed BufferedImage
     */
    public BufferedImage process(BufferedImage input, ProcessingParameters params) {
        if (input == null) return null;
        if (params == null) return input;

        // Base contrast/brightness adjustment if tuned
        BufferedImage working = input;
        if (params.getContrast() != 1.0 || params.getBrightness() != 0.0) {
            working = contrastProcessor.process(working, params);
        }

        return switch (params.getMode()) {
            case ORIGINAL -> working;
            case GRAYSCALE -> grayscaleProcessor.process(working, params);
            case EDGE_DETECTION -> edgeProcessor.process(working, params);
            case THRESHOLD -> thresholdProcessor.process(working, params);
            case BLUR -> blurProcessor.process(working, params);
            case SHARPEN -> sharpen(working);
            case CONTRAST -> contrastProcessor.process(working, params);
            case VEHICLE_DETECTION -> applyPipelineFilters(working, params);
        };
    }

    /**
     * Chains active filter checkboxes for vehicle detection preview.
     */
    private BufferedImage applyPipelineFilters(BufferedImage input, ProcessingParameters params) {
        BufferedImage result = input;
        if (params.isGrayscale()) {
            result = grayscaleProcessor.process(result, params);
        }
        if (params.isNoiseReduction()) {
            result = blurProcessor.process(result, params);
        }
        if (params.isEdgeDetection()) {
            result = edgeProcessor.process(result, params);
        }
        if (params.isContrastEnhancement()) {
            result = contrastProcessor.process(result, params);
        }
        return result;
    }

    /**
     * Applies a 3x3 unsharp masking kernel to improve edge definition.
     */
    public BufferedImage sharpen(BufferedImage input) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // 3x3 Sharpen Kernel: center 5, cross -1
        int[][] kernel = {
                { 0, -1,  0},
                {-1,  5, -1},
                { 0, -1,  0}
        };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    output.setRGB(x, y, input.getRGB(x, y));
                    continue;
                }

                int rSum = 0, gSum = 0, bSum = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = input.getRGB(x + kx, y + ky);
                        int w = kernel[ky + 1][kx + 1];
                        rSum += ((rgb >> 16) & 0xFF) * w;
                        gSum += ((rgb >> 8) & 0xFF) * w;
                        bSum += (rgb & 0xFF) * w;
                    }
                }

                int r = Math.max(0, Math.min(255, rSum));
                int g = Math.max(0, Math.min(255, gSum));
                int b = Math.max(0, Math.min(255, bSum));
                output.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return output;
    }
}
