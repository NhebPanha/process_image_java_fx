package com.rupp.trafficvision.detection;

import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.ProcessingParameters;
import java.awt.image.BufferedImage;

/**
 * Abstraction for vehicle detection and classification engines.
 * Enables interchangeable implementations (e.g., ImageProcessing, OpenCV, YOLO, ONNX)
 * without altering the JavaFX dashboard or service layer.
 */
public interface VehicleDetector {

    /**
     * Analyzes the supplied image and returns the detected vehicle and bounding box.
     *
     * @param image source image
     * @return detection result
     */
    DetectionResult detect(BufferedImage image);

    /**
     * Analyzes the supplied image with custom preprocessing parameters.
     *
     * @param image  source image
     * @param params active preprocessing tunables
     * @return detection result
     */
    DetectionResult detect(BufferedImage image, ProcessingParameters params);
}
