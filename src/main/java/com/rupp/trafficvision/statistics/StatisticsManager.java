package com.rupp.trafficvision.statistics;

import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.VehicleType;
import com.rupp.trafficvision.sync.SynchronizationStrategy;
import com.rupp.trafficvision.sync.lock.ProcessingLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe statistics engine that tracks aggregated traffic analysis metrics.
 * Critical metrics updates are guarded by a pluggable {@link SynchronizationStrategy}.
 */
public class StatisticsManager {

    private int totalImages = 0;
    private int processedImages = 0;
    private int successfulDetections = 0;
    private int failedDetections = 0;

    private long totalProcessingTime = 0;
    private long fastestProcessing = Long.MAX_VALUE;
    private long slowestProcessing = 0;

    private final Map<VehicleType, Integer> vehicleCounts = new EnumMap<>(VehicleType.class);
    private final List<DetectionResult> history = new ArrayList<>();

    private SynchronizationStrategy synchronizationStrategy;

    public StatisticsManager(SynchronizationStrategy strategy) {
        this.synchronizationStrategy = strategy != null ? strategy : new ProcessingLock();
        for (VehicleType type : VehicleType.values()) {
            vehicleCounts.put(type, 0);
        }
    }

    public StatisticsManager() {
        this(new ProcessingLock());
    }

    public synchronized SynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }

    public synchronized void setSynchronizationStrategy(SynchronizationStrategy strategy) {
        if (strategy != null) {
            this.synchronizationStrategy = strategy;
        }
    }

    /**
     * Increments the total submitted image count.
     */
    public void recordImageSubmitted() {
        try {
            synchronizationStrategy.executeWithProtection(() -> {
                totalImages++;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Records the completion of a detection result and updates all metric counters.
     *
     * @param result the detection result to record
     */
    public void recordResult(DetectionResult result) {
        if (result == null) return;
        try {
            synchronizationStrategy.executeWithProtection(() -> {
                processedImages++;
                history.add(result);

                long duration = result.getProcessingTime();
                totalProcessingTime += duration;

                if (duration < fastestProcessing) {
                    fastestProcessing = duration;
                }
                if (duration > slowestProcessing) {
                    slowestProcessing = duration;
                }

                if (result.isDetected()) {
                    successfulDetections++;
                    VehicleType type = result.getVehicleType();
                    vehicleCounts.put(type, vehicleCounts.getOrDefault(type, 0) + 1);
                } else {
                    failedDetections++;
                    vehicleCounts.put(VehicleType.UNKNOWN, vehicleCounts.getOrDefault(VehicleType.UNKNOWN, 0) + 1);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Resets all accumulated statistics to zero.
     */
    public void reset() {
        try {
            synchronizationStrategy.executeWithProtection(() -> {
                totalImages = 0;
                processedImages = 0;
                successfulDetections = 0;
                failedDetections = 0;
                totalProcessingTime = 0;
                fastestProcessing = Long.MAX_VALUE;
                slowestProcessing = 0;
                for (VehicleType type : VehicleType.values()) {
                    vehicleCounts.put(type, 0);
                }
                history.clear();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized int getTotalImages() {
        return totalImages;
    }

    public synchronized int getProcessedImages() {
        return processedImages;
    }

    public synchronized int getSuccessfulDetections() {
        return successfulDetections;
    }

    public synchronized int getFailedDetections() {
        return failedDetections;
    }

    public synchronized double getAverageProcessingTime() {
        return processedImages > 0 ? (double) totalProcessingTime / processedImages : 0.0;
    }

    public synchronized long getFastestProcessing() {
        return fastestProcessing == Long.MAX_VALUE ? 0 : fastestProcessing;
    }

    public synchronized long getSlowestProcessing() {
        return slowestProcessing;
    }

    public synchronized int getCount(VehicleType type) {
        return vehicleCounts.getOrDefault(type, 0);
    }

    public synchronized int getCarsDetected() {
        return getCount(VehicleType.CAR);
    }

    public synchronized int getMotorcyclesDetected() {
        return getCount(VehicleType.MOTORCYCLE);
    }

    public synchronized int getBicyclesDetected() {
        return getCount(VehicleType.BICYCLE);
    }

    public synchronized int getTricyclesDetected() {
        return getCount(VehicleType.TRICYCLE);
    }

    public synchronized int getMopedsDetected() {
        return getCount(VehicleType.MOPED);
    }

    public synchronized Map<VehicleType, Integer> getVehicleDistribution() {
        return new EnumMap<>(vehicleCounts);
    }

    public synchronized List<DetectionResult> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    @Override
    public synchronized String toString() {
        return String.format("""
                Total Images:        %d
                Processed:           %d
                Successful:          %d
                Failed:              %d
                Average Time:        %.1f ms
                Fastest Time:        %d ms
                Slowest Time:        %d ms

                Cars:                %d
                Motorcycles:         %d
                Bicycles:            %d
                Tricycles:           %d
                Mopeds:              %d""",
                totalImages, processedImages, successfulDetections, failedDetections,
                getAverageProcessingTime(), getFastestProcessing(), getSlowestProcessing(),
                getCarsDetected(), getMotorcyclesDetected(), getBicyclesDetected(),
                getTricyclesDetected(), getMopedsDetected());
    }
}
