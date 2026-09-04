package com.turbotech.cvstudio.pipeline;

import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;
import com.turbotech.cvstudio.util.EventLog;
import javafx.application.Platform;
import org.opencv.core.Mat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Coordinates execution of pipeline stages on a dedicated background thread.
 * Implements frame-dropping to maintain real-time performance on high-frame-rate inputs,
 * records per-stage execution times, and passes completed frames to the JavaFX thread.
 */
public class PipelineRunner {

    private final List<PipelineStage> stages;
    private final PipelineSettings settings;
    private final Consumer<Frame> onComplete;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cv-pipeline");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private Mat lastSourceImage = null;

    public PipelineRunner(List<PipelineStage> stages, PipelineSettings settings, Consumer<Frame> onComplete) {
        this.stages = stages;
        this.settings = settings;
        this.onComplete = onComplete;
    }

    /**
     * Submits a new frame into the pipeline. If the pipeline is currently busy, the frame is
     * dropped immediately to guarantee zero queuing latency for live video streams.
     */
    public void submit(Frame frame) {
        if (frame == null || frame.getOriginal() == null || frame.getOriginal().empty()) {
            if (frame != null) frame.release();
            return;
        }

        // Cache a clone of original image for parameter re-evaluations
        synchronized (this) {
            if (lastSourceImage != null) {
                lastSourceImage.release();
            }
            lastSourceImage = frame.getOriginal().clone();
        }

        if (!busy.compareAndSet(false, true)) {
            // Drop frame to keep live video real-time
            frame.release();
            return;
        }

        executor.submit(() -> processFrame(frame));
    }

    /**
     * Re-runs the entire pipeline on the most recent source image (used when tuning sliders live).
     */
    public void reprocessCurrent() {
        Mat srcCopy;
        synchronized (this) {
            if (lastSourceImage == null || lastSourceImage.empty()) {
                return;
            }
            srcCopy = lastSourceImage.clone();
        }

        Frame frame = new Frame(srcCopy);
        if (!busy.compareAndSet(false, true)) {
            frame.release();
            return;
        }

        executor.submit(() -> processFrame(frame));
    }

    private void processFrame(Frame frame) {
        try {
            Frame current = frame;
            for (PipelineStage stage : stages) {
                if (!stage.enabled(settings)) {
                    continue;
                }
                long t0 = System.nanoTime();
                current = stage.apply(current, settings);
                long elapsedMs = Math.max(0, (System.nanoTime() - t0) / 1_000_000);
                current.addTiming(stage.name(), elapsedMs);
            }

            final Frame completed = current;
            Platform.runLater(() -> onComplete.accept(completed));
        } catch (Exception ex) {
            Platform.runLater(() -> EventLog.error("Pipeline execution error: " + ex.getMessage()));
        } finally {
            busy.set(false);
        }
    }

    public boolean isBusy() {
        return busy.get();
    }

    public synchronized void shutdown() {
        if (lastSourceImage != null) {
            lastSourceImage.release();
            lastSourceImage = null;
        }
        executor.shutdownNow();
    }
}
