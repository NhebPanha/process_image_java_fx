package com.turbotech.cvstudio.core;

import com.turbotech.cvstudio.util.EventLog;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.function.Consumer;

/**
 * JavaFX Service for capturing live camera video frames off the UI thread.
 */
public class CameraService extends Service<Void> {

    private final IntegerProperty deviceIndex = new SimpleIntegerProperty(0);
    private final IntegerProperty frameWidth = new SimpleIntegerProperty(1280);
    private final IntegerProperty frameHeight = new SimpleIntegerProperty(720);
    private final BooleanProperty streaming = new SimpleBooleanProperty(false);
    private final Consumer<Mat> frameConsumer;
    private volatile boolean running = false;

    public CameraService(Consumer<Mat> frameConsumer) {
        this.frameConsumer = frameConsumer;
    }

    public IntegerProperty deviceIndexProperty() { return deviceIndex; }
    public int getDeviceIndex() { return deviceIndex.get(); }
    public void setDeviceIndex(int idx) { deviceIndex.set(idx); }

    public IntegerProperty frameWidthProperty() { return frameWidth; }
    public IntegerProperty frameHeightProperty() { return frameHeight; }

    public BooleanProperty streamingProperty() { return streaming; }
    public boolean isStreaming() { return streaming.get(); }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                NativeLoader.load();
                int dev = deviceIndex.get();
                EventLog.info("Attempting to open camera device #" + dev + "...");

                VideoCapture capture = new VideoCapture(dev);
                capture.set(Videoio.CAP_PROP_FRAME_WIDTH, frameWidth.get());
                capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, frameHeight.get());

                if (!capture.isOpened()) {
                    String msg = "Camera #" + dev + " is unavailable or not connected.";
                    updateMessage(msg);
                    EventLog.warn(msg);
                    streaming.set(false);
                    return null;
                }

                running = true;
                streaming.set(true);
                EventLog.success("Camera #" + dev + " opened successfully (" + frameWidth.get() + "x" + frameHeight.get() + ").");

                Mat buffer = new Mat();
                try {
                    while (running && !isCancelled()) {
                        if (capture.read(buffer) && !buffer.empty()) {
                            // Forward cloned frame to consumer; consumer must release it when finished
                            frameConsumer.accept(buffer.clone());
                        }
                        Thread.sleep(16); // Target ~60 FPS cap
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    EventLog.error("Camera error: " + ex.getMessage());
                } finally {
                    running = false;
                    streaming.set(false);
                    capture.release();
                    buffer.release();
                    EventLog.info("Camera #" + dev + " stopped and released.");
                }
                return null;
            }
        };
    }

    public void stopCamera() {
        running = false;
        cancel();
        reset();
    }
}
