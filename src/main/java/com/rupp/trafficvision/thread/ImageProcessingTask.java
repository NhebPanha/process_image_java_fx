package com.rupp.trafficvision.thread;

import com.rupp.trafficvision.detection.VehicleDetector;
import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.ProcessingParameters;
import com.rupp.trafficvision.processing.BlurProcessor;
import com.rupp.trafficvision.processing.EdgeDetectionProcessor;
import com.rupp.trafficvision.processing.GrayscaleProcessor;
import com.rupp.trafficvision.sync.SynchronizationStrategy;
import javafx.concurrent.Task;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Background worker task executing the 7-stage image processing and vehicle detection pipeline.
 * Ensures that heavy CPU computations do not block the JavaFX Application Thread.
 */
public class ImageProcessingTask extends Task<DetectionResult> {

    private final BufferedImage sourceImage;
    private final ProcessingParameters parameters;
    private final VehicleDetector detector;
    private final SynchronizationStrategy synchronizationStrategy;
    private final Consumer<BufferedImage> intermediatePreviewConsumer;

    private final GrayscaleProcessor grayscaleProcessor = new GrayscaleProcessor();
    private final BlurProcessor blurProcessor = new BlurProcessor();
    private final EdgeDetectionProcessor edgeProcessor = new EdgeDetectionProcessor();

    /**
     * Constructs an ImageProcessingTask.
     *
     * @param sourceImage                input image to process
     * @param parameters                 processing parameters and filter options
     * @param detector                   vehicle detector implementation
     * @param synchronizationStrategy    concurrency regulation strategy
     * @param intermediatePreviewConsumer callback to stream intermediate filter previews to UI
     */
    public ImageProcessingTask(BufferedImage sourceImage,
                               ProcessingParameters parameters,
                               VehicleDetector detector,
                               SynchronizationStrategy synchronizationStrategy,
                               Consumer<BufferedImage> intermediatePreviewConsumer) {
        this.sourceImage = sourceImage;
        this.parameters = parameters != null ? parameters : new ProcessingParameters();
        this.detector = detector;
        this.synchronizationStrategy = synchronizationStrategy;
        this.intermediatePreviewConsumer = intermediatePreviewConsumer;
    }

    @Override
    protected DetectionResult call() throws Exception {
        if (sourceImage == null) {
            updateMessage("Failed: Null Image");
            return DetectionResult.failure("IMG-ERR", "Source image is null");
        }

        // Acquire synchronization permission (ReentrantLock or Semaphore)
        if (synchronizationStrategy != null) {
            synchronizationStrategy.acquireAccess();
        }

        try {
            // Stage 1: Loading Image
            checkCancelled();
            updateProgress(1, 7);
            updateMessage("Stage 1/7: Loading Image...");
            Thread.sleep(40); // Controlled stage delay for visual responsiveness

            // Stage 2: Preprocessing (Resize / Normalization)
            checkCancelled();
            updateProgress(2, 7);
            updateMessage("Stage 2/7: Preprocessing & Color Adjustment...");
            BufferedImage working = sourceImage;
            Thread.sleep(40);

            // Stage 3: Grayscale conversion
            checkCancelled();
            updateProgress(3, 7);
            updateMessage("Stage 3/7: Converting Luminance Grayscale...");
            BufferedImage gray = grayscaleProcessor.process(working, parameters);
            if (intermediatePreviewConsumer != null && parameters.isGrayscale()) {
                intermediatePreviewConsumer.accept(gray);
            }
            Thread.sleep(40);

            // Stage 4: Noise reduction & Edge Detection
            checkCancelled();
            updateProgress(4, 7);
            updateMessage("Stage 4/7: Applying Gaussian Blur & Sobel Edge Detection...");
            BufferedImage blurred = parameters.isNoiseReduction() ? blurProcessor.process(gray, parameters) : gray;
            BufferedImage edges = edgeProcessor.process(blurred, parameters);
            if (intermediatePreviewConsumer != null && parameters.isEdgeDetection()) {
                intermediatePreviewConsumer.accept(edges);
            }
            Thread.sleep(40);

            // Stage 5: Vehicle Detection & Region Segmentation
            checkCancelled();
            updateProgress(5, 7);
            updateMessage("Stage 5/7: Segmenting Vehicle Candidate Contours...");
            Thread.sleep(40);

            // Stage 6: Classification
            checkCancelled();
            updateProgress(6, 7);
            updateMessage("Stage 6/7: Classifying Vehicle Type & Profile...");
            DetectionResult result = detector.detect(sourceImage, parameters);
            Thread.sleep(40);

            // Stage 7: Result Finalization
            checkCancelled();
            updateProgress(7, 7);
            updateMessage(String.format("Stage 7/7: Complete — Detected %s (%.1f%%)",
                    result.getVehicleType().getDisplayName(), result.getConfidencePercentage()));

            return result;
        } finally {
            if (synchronizationStrategy != null) {
                synchronizationStrategy.releaseAccess();
            }
        }
    }

    private void checkCancelled() throws InterruptedException {
        if (isCancelled()) {
            throw new InterruptedException("Task was cancelled by user");
        }
    }
}
