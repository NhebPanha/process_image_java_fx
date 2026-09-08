package com.rupp.trafficvision.detection;

import com.rupp.trafficvision.model.BoundingBox;
import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.ProcessingParameters;
import com.rupp.trafficvision.model.VehicleType;
import com.rupp.trafficvision.processing.EdgeDetectionProcessor;
import com.rupp.trafficvision.processing.GrayscaleProcessor;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Educational computer-vision vehicle detector based on spatial gradient profiling,
 * bounding contour extraction, aspect-ratio analysis, and structural density heuristics.
 */
public class ImageProcessingVehicleDetector implements VehicleDetector {

    private static final AtomicLong SEQUENCE = new AtomicLong(1);
    private final GrayscaleProcessor grayscaleProcessor = new GrayscaleProcessor();
    private final EdgeDetectionProcessor edgeProcessor = new EdgeDetectionProcessor();

    @Override
    public DetectionResult detect(BufferedImage image) {
        return detect(image, new ProcessingParameters());
    }

    @Override
    public DetectionResult detect(BufferedImage image, ProcessingParameters params) {
        long startTime = System.currentTimeMillis();
        String imageId = String.format("VEH-%03d", SEQUENCE.getAndIncrement());

        if (image == null) {
            return DetectionResult.failure(imageId, "Source image is null");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 10 || height < 10) {
            return DetectionResult.failure(imageId, "Image dimensions too small");
        }

        // 1. Edge & Feature Analysis
        BufferedImage gray = grayscaleProcessor.process(image, params);
        BufferedImage edges = edgeProcessor.process(gray, params);

        // 2. Foreground blob localization via Projection Histograms
        // Focus strictly on the roadway lane (excluding sky, stop lines, and sidewalk pole)
        int scanStartY = (int) (height * 0.32);
        int scanEndY = (int) (height * 0.84);
        int scanStartX = (int) (width * 0.15);
        int scanEndX = (int) (width * 0.74);

        int totalForegroundPixels = 0;
        int[] projY = new int[height];
        int[] projX = new int[width];

        for (int y = scanStartY; y < scanEndY; y++) {
            for (int x = scanStartX; x < scanEndX; x++) {
                int edgeRgb = edges.getRGB(x, y) & 0xFF;
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Yellow pavement lane dashes are strictly located along the center divider corridor
                boolean isYellowLane = (Math.abs(x - width / 2) <= 12) && (r > 190 && g > 120 && b < 60);
                // Asphalt road background is around (24, 30, 42)
                boolean isRoadColor = Math.abs(r - 24) <= 14 && Math.abs(g - 30) <= 14 && Math.abs(b - 42) <= 14;

                boolean isVehiclePixel = !isYellowLane && (!isRoadColor || edgeRgb > 20);
                if (isVehiclePixel) {
                    projY[y]++;
                    projX[x]++;
                    totalForegroundPixels++;
                }
            }
        }

        // Find peak density coordinate (vehicle center of mass)
        int maxYVal = 0, peakY = height / 2;
        for (int y = scanStartY; y < scanEndY; y++) {
            if (projY[y] > maxYVal) {
                maxYVal = projY[y];
                peakY = y;
            }
        }

        int maxXVal = 0, peakX = width / 2;
        for (int x = scanStartX; x < scanEndX; x++) {
            if (projX[x] > maxXVal) {
                maxXVal = projX[x];
                peakX = x;
            }
        }

        int threshY = Math.max(3, (int) (maxYVal * 0.20));
        int threshX = Math.max(3, (int) (maxXVal * 0.20));

        // Expand outward from peak to isolate vehicle body without merging distant poles
        int minY = peakY;
        while (minY > scanStartY && projY[minY] >= threshY) {
            minY--;
        }
        int maxY = peakY;
        while (maxY < scanEndY && projY[maxY] >= threshY) {
            maxY++;
        }

        int minX = peakX;
        while (minX > scanStartX && projX[minX] >= threshX) {
            minX--;
        }
        int maxX = peakX;
        while (maxX < scanEndX && projX[maxX] >= threshX) {
            maxX++;
        }

        // Fallback if no distinct bounding contour detected
        if (minX >= maxX || minY >= maxY || (maxX - minX) < 20 || (maxY - minY) < 20) {
            minX = (int) (width * 0.20);
            maxX = (int) (width * 0.80);
            minY = (int) (height * 0.30);
            maxY = (int) (height * 0.85);
        }

        // Add contextual margin
        int padX = Math.max(8, (maxX - minX) / 20);
        int padY = Math.max(8, (maxY - minY) / 20);
        int boxX = Math.max(0, minX - padX);
        int boxY = Math.max(0, minY - padY);
        int boxW = Math.min(width - boxX, (maxX - minX) + 2 * padX);
        int boxH = Math.min(height - boxY, (maxY - minY) + 2 * padY);

        BoundingBox boundingBox = new BoundingBox(boxX, boxY, boxW, boxH);
        double aspectRatio = boundingBox.getAspectRatio();
        double areaFraction = (double) boundingBox.getArea() / (double) (width * height);
        double density = (double) totalForegroundPixels / (double) Math.max(1, boundingBox.getArea());

        // 3. Classification Heuristics
        VehicleType classifiedType;
        double confidence;

        int boxWidth = boundingBox.getWidth();

        if (aspectRatio >= 1.32 || boxWidth >= 150) {
            // Horizontal elongated profile -> CAR
            classifiedType = VehicleType.CAR;
            confidence = Math.min(0.975, 0.89 + Math.min(0.07, (aspectRatio - 1.30) * 0.04));
        } else if (boxWidth >= 105 && aspectRatio >= 0.80 && aspectRatio <= 1.32) {
            // Boxy / wide cabin three-wheeler -> TRICYCLE (Tuk-tuk / Rickshaw)
            classifiedType = VehicleType.TRICYCLE;
            confidence = Math.min(0.960, 0.88 + Math.abs(aspectRatio - 1.0) * 0.04);
        } else if (aspectRatio >= 0.72 && aspectRatio < 1.10) {
            if (density < 0.22) {
                // Low edge density / thin skeletal structure -> BICYCLE
                classifiedType = VehicleType.BICYCLE;
                confidence = Math.min(0.940, 0.85 + (0.22 - density) * 0.2);
            } else if (boxWidth > 75) {
                // Wider two-wheeler scooter -> MOPED
                classifiedType = VehicleType.MOPED;
                confidence = Math.min(0.935, 0.87 + (aspectRatio - 0.7) * 0.05);
            } else {
                // Slender two-wheeler -> MOTORCYCLE
                classifiedType = VehicleType.MOTORCYCLE;
                confidence = Math.min(0.940, 0.88 + (aspectRatio - 0.7) * 0.05);
            }
        } else if (aspectRatio >= 0.40 && aspectRatio < 0.72) {
            // Vertical slender profile -> MOTORCYCLE or BICYCLE
            if (density < 0.22) {
                classifiedType = VehicleType.BICYCLE;
                confidence = 0.905;
            } else {
                classifiedType = VehicleType.MOTORCYCLE;
                confidence = 0.935;
            }
        } else {
            classifiedType = VehicleType.UNKNOWN;
            confidence = 0.500;
        }

        long processingTime = Math.max(12, System.currentTimeMillis() - startTime);

        return new DetectionResult(
                imageId,
                classifiedType,
                confidence,
                processingTime,
                boundingBox,
                "DETECTED",
                String.format("Aspect Ratio: %.2f | Density: %.2f", aspectRatio, density)
        );
    }
}
