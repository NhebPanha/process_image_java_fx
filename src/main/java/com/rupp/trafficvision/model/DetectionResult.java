package com.rupp.trafficvision.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates the output of a vehicle detection operation, including classified
 * vehicle type, detection confidence, execution duration, and spatial bounding box.
 */
public class DetectionResult {
    private final String imageId;
    private final VehicleType vehicleType;
    private final double confidence;
    private final long processingTime;
    private final BoundingBox boundingBox;
    private final String status;
    private final LocalDateTime timestamp;
    private final String details;

    /**
     * Constructs a full DetectionResult.
     *
     * @param imageId        unique identifier of the processed image frame
     * @param vehicleType    detected vehicle category
     * @param confidence     confidence score (0.0 to 1.0)
     * @param processingTime processing duration in milliseconds
     * @param boundingBox    bounding box coordinates
     * @param status         status string (e.g. "DETECTED", "FAILED")
     * @param details        additional diagnostic or stage information
     */
    public DetectionResult(String imageId, VehicleType vehicleType, double confidence,
                           long processingTime, BoundingBox boundingBox, String status, String details) {
        this.imageId = imageId != null ? imageId : "IMG-" + System.currentTimeMillis() % 10000;
        this.vehicleType = vehicleType != null ? vehicleType : VehicleType.UNKNOWN;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.processingTime = processingTime;
        this.boundingBox = boundingBox != null ? boundingBox : new BoundingBox(0, 0, 100, 100);
        this.status = status != null ? status : "DETECTED";
        this.timestamp = LocalDateTime.now();
        this.details = details != null ? details : "";
    }

    /**
     * Creates an empty/failed detection result.
     *
     * @param imageId unique image identifier
     * @param reason  failure reason
     * @return failed DetectionResult
     */
    public static DetectionResult failure(String imageId, String reason) {
        return new DetectionResult(
                imageId,
                VehicleType.UNKNOWN,
                0.0,
                0,
                new BoundingBox(0, 0, 0, 0),
                "FAILED",
                reason
        );
    }

    public String getImageId() {
        return imageId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getConfidencePercentage() {
        return confidence * 100.0;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }

    public boolean isDetected() {
        return "DETECTED".equalsIgnoreCase(status) && vehicleType != VehicleType.UNKNOWN;
    }

    /**
     * Formats detection result summary matching standard report formatting.
     */
    public String toReportString() {
        return String.format("""
                ----------------------------------
                        DETECTION RESULT
                ----------------------------------
                Image ID:      %s
                Vehicle:       %s
                Confidence:    %.1f%%
                Processing:    %d ms

                Bounding Box:
                X: %d
                Y: %d
                Width: %d
                Height: %d

                Status: %s
                ----------------------------------""",
                imageId,
                vehicleType.name(),
                getConfidencePercentage(),
                processingTime,
                boundingBox.getX(),
                boundingBox.getY(),
                boundingBox.getWidth(),
                boundingBox.getHeight(),
                status
        );
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%.1f%%, %dms)", imageId, vehicleType, getConfidencePercentage(), processingTime);
    }
}
