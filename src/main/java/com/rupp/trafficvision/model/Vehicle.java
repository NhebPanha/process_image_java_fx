package com.rupp.trafficvision.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a monitored vehicle instance tracked by the system.
 * Provides JavaFX observable properties for direct TableView binding.
 */
public class Vehicle {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final StringProperty vehicleId;
    private final ObjectProperty<VehicleType> vehicleType;
    private final DoubleProperty confidence;
    private final LongProperty processingTime;
    private final StringProperty threadName;
    private final StringProperty status;
    private final ObjectProperty<LocalDateTime> timestamp;
    private final StringProperty imagePath;
    private final ObjectProperty<BoundingBox> boundingBox;

    /**
     * Constructs a Vehicle record.
     *
     * @param vehicleId      unique vehicle or frame tracking identifier
     * @param vehicleType    detected category
     * @param confidence     detection confidence (0.0 to 1.0)
     * @param processingTime execution duration in ms
     * @param threadName     name of worker thread executing detection
     * @param status         status string (e.g., "DETECTED", "FAILED")
     * @param timestamp      occurrence timestamp
     * @param imagePath      source image path
     * @param boundingBox    bounding box
     */
    public Vehicle(String vehicleId, VehicleType vehicleType, double confidence,
                   long processingTime, String threadName, String status,
                   LocalDateTime timestamp, String imagePath, BoundingBox boundingBox) {
        this.vehicleId = new SimpleStringProperty(vehicleId);
        this.vehicleType = new SimpleObjectProperty<>(vehicleType);
        this.confidence = new SimpleDoubleProperty(confidence);
        this.processingTime = new SimpleLongProperty(processingTime);
        this.threadName = new SimpleStringProperty(threadName != null ? threadName : "Worker");
        this.status = new SimpleStringProperty(status);
        this.timestamp = new SimpleObjectProperty<>(timestamp != null ? timestamp : LocalDateTime.now());
        this.imagePath = new SimpleStringProperty(imagePath != null ? imagePath : "");
        this.boundingBox = new SimpleObjectProperty<>(boundingBox);
    }

    public static Vehicle fromDetectionResult(DetectionResult result, String threadName, String imagePath) {
        return new Vehicle(
                result.getImageId(),
                result.getVehicleType(),
                result.getConfidence(),
                result.getProcessingTime(),
                threadName,
                result.getStatus(),
                result.getTimestamp(),
                imagePath,
                result.getBoundingBox()
        );
    }

    public String getVehicleId() {
        return vehicleId.get();
    }

    public StringProperty vehicleIdProperty() {
        return vehicleId;
    }

    public VehicleType getVehicleType() {
        return vehicleType.get();
    }

    public ObjectProperty<VehicleType> vehicleTypeProperty() {
        return vehicleType;
    }

    public double getConfidence() {
        return confidence.get();
    }

    public DoubleProperty confidenceProperty() {
        return confidence;
    }

    public String getConfidenceFormatted() {
        return String.format("%.1f%%", confidence.get() * 100.0);
    }

    public long getProcessingTime() {
        return processingTime.get();
    }

    public LongProperty processingTimeProperty() {
        return processingTime;
    }

    public String getProcessingTimeFormatted() {
        return processingTime.get() + " ms";
    }

    public String getThreadName() {
        return threadName.get();
    }

    public StringProperty threadNameProperty() {
        return threadName;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp.get();
    }

    public ObjectProperty<LocalDateTime> timestampProperty() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.get().format(TIME_FMT);
    }

    public String getImagePath() {
        return imagePath.get();
    }

    public StringProperty imagePathProperty() {
        return imagePath;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox.get();
    }

    public ObjectProperty<BoundingBox> boundingBoxProperty() {
        return boundingBox;
    }
}
