package com.rupp.trafficvision.service;

import com.rupp.trafficvision.detection.ImageProcessingVehicleDetector;
import com.rupp.trafficvision.detection.VehicleDetector;
import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.ProcessingParameters;
import com.rupp.trafficvision.model.ThreadStatus;
import com.rupp.trafficvision.sync.SynchronizationStrategy;
import com.rupp.trafficvision.sync.lock.ProcessingLock;
import com.rupp.trafficvision.thread.ImageProcessingTask;
import com.rupp.trafficvision.thread.ProcessingWorker;
import com.rupp.trafficvision.util.LoggerUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service managing background thread execution of vehicle detection tasks.
 * Ensures the JavaFX Application Thread remains responsive and tracks worker states.
 */
public class VehicleDetectionService {

    private final ExecutorService executorService;
    private final List<ProcessingWorker> workerRegistry = new CopyOnWriteArrayList<>();
    private VehicleDetector vehicleDetector;
    private SynchronizationStrategy synchronizationStrategy;
    private final StatisticsService statisticsService;

    public VehicleDetectionService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        this.vehicleDetector = new ImageProcessingVehicleDetector();
        this.synchronizationStrategy = new ProcessingLock();

        // Dedicated ThreadFactory creating named worker threads
        AtomicInteger threadCount = new AtomicInteger(1);
        int poolSize = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));

        this.executorService = Executors.newFixedThreadPool(poolSize, r -> {
            int id = threadCount.getAndIncrement();
            String name = "Worker-" + id;
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        });

        // Initialize worker registry metadata
        for (int i = 1; i <= poolSize; i++) {
            workerRegistry.add(new ProcessingWorker("Worker-" + i, i));
        }
    }

    public synchronized VehicleDetector getVehicleDetector() {
        return vehicleDetector;
    }

    public synchronized void setVehicleDetector(VehicleDetector detector) {
        if (detector != null) {
            this.vehicleDetector = detector;
            LoggerUtil.info("Vehicle detector switched to: " + detector.getClass().getSimpleName());
        }
    }

    public synchronized SynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }

    public synchronized void setSynchronizationStrategy(SynchronizationStrategy strategy) {
        if (strategy != null) {
            this.synchronizationStrategy = strategy;
            if (statisticsService != null) {
                statisticsService.getStatisticsManager().setSynchronizationStrategy(strategy);
            }
            LoggerUtil.info("Synchronization strategy switched to: " + strategy.getStrategyName());
        }
    }

    /**
     * Creates and submits an asynchronous {@link ImageProcessingTask} to the executor.
     *
     * @param image       input image
     * @param params      tunable parameters
     * @param previewHook callback for streaming intermediate previews
     * @return active ImageProcessingTask
     */
    public ImageProcessingTask submitDetectionTask(BufferedImage image,
                                                   ProcessingParameters params,
                                                   Consumer<BufferedImage> previewHook) {
        if (statisticsService != null) {
            statisticsService.getStatisticsManager().recordImageSubmitted();
        }

        ImageProcessingTask task = new ImageProcessingTask(
                image,
                params,
                vehicleDetector,
                synchronizationStrategy,
                previewHook
        );

        // Update worker status tracking
        ProcessingWorker worker = getAvailableWorker();
        if (worker != null) {
            worker.updateState(ThreadStatus.PROCESSING, "Detecting Vehicle", 0);
        }

        task.setOnSucceeded(e -> {
            DetectionResult result = task.getValue();
            if (statisticsService != null) {
                statisticsService.getStatisticsManager().recordResult(result);
                statisticsService.notifyStatisticsChanged();
            }
            if (worker != null) {
                worker.updateState(ThreadStatus.COMPLETED, "Completed " + result.getImageId(), result.getProcessingTime());
            }
        });

        task.setOnFailed(e -> {
            if (worker != null) {
                worker.updateState(ThreadStatus.ERROR, "Task Failed", 0);
            }
            LoggerUtil.error("Detection task failed: " + task.getException());
        });

        task.setOnCancelled(e -> {
            if (worker != null) {
                worker.updateState(ThreadStatus.IDLE, "Task Cancelled", 0);
            }
        });

        executorService.submit(task);
        return task;
    }

    private ProcessingWorker getAvailableWorker() {
        for (ProcessingWorker w : workerRegistry) {
            if (w.getStatus() == ThreadStatus.IDLE || w.getStatus() == ThreadStatus.COMPLETED) {
                return w;
            }
        }
        return workerRegistry.isEmpty() ? null : workerRegistry.get(0);
    }

    public List<ProcessingWorker> getWorkerRegistry() {
        return Collections.unmodifiableList(workerRegistry);
    }

    /**
     * Gracefully shuts down the executor pool.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
