package com.nhebpanha.cvstudio;

import com.nhebpanha.cvstudio.core.CameraService;
import com.nhebpanha.cvstudio.core.ModelService;
import com.nhebpanha.cvstudio.core.NativeLoader;
import com.nhebpanha.cvstudio.core.SampleImages;
import com.nhebpanha.cvstudio.model.Frame;
import com.nhebpanha.cvstudio.model.PipelineSettings;
import com.nhebpanha.cvstudio.pipeline.*;
import com.nhebpanha.cvstudio.ui.MainView;
import com.nhebpanha.cvstudio.util.EventLog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main application entry point for CV Studio.
 * Boots OpenCV native libraries, initializes the 5-stage vision pipeline, sets
 * up UI,
 * and manages background lifecycle.
 *
 * NOTE: App itself does NOT extend Application directly. Instead, it delegates
 * to CVApp,
 * allowing it to be executed directly from VS Code ("Run Java" on App.java) and
 * standard
 * classpaths without triggering "Error: JavaFX runtime components are missing".
 */
public class App {

    public static void main(String[] args) {
        Application.launch(CVApp.class, args);
    }

    public static class CVApp extends Application {
        private PipelineRunner runner;
        private CameraService camera;
        private ModelService models;
        private PipelineSettings settings;
        private MainView mainView;

        @Override
        public void init() {
            EventLog.info("Initializing CV Studio...");
            NativeLoader.load();
            models = new ModelService();
        }

        @Override
        public void start(Stage stage) {
            settings = new PipelineSettings();

            // 1. Instantiate Main View
            mainView = new MainView(settings, () -> {
                // Callback when any tunable setting (slider, checkbox) changes
                if (runner != null) {
                    runner.reprocessCurrent();
                }
            });

            // 2. Build 5 Pipeline Stages
            AcquisitionStage stage1 = new AcquisitionStage();
            PreprocessStage stage2 = new PreprocessStage();
            FeatureExtractionStage stage3 = new FeatureExtractionStage();
            RecognitionStage stage4 = new RecognitionStage(models);
            DecisionStage stage5 = new DecisionStage();

            // 3. Assemble Pipeline Runner
            runner = new PipelineRunner(
                    List.of(stage1, stage2, stage3, stage4, stage5),
                    settings,
                    mainView::update);

            // 4. Set up Camera Service
            camera = new CameraService(mat -> runner.submit(new Frame(mat)));
            mainView.bindCamera(camera);

            // 5. Wire action handlers
            mainView.setOpenImageHandler(this::loadImageFile);
            mainView.setSampleSelectedHandler(this::loadSampleImage);
            mainView.setRunHandler(() -> runner.reprocessCurrent());

            // 6. Assemble Scene
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double initialWidth = Math.min(1366, screenBounds.getWidth());
            double initialHeight = Math.min(768, screenBounds.getHeight());
            Scene scene = new Scene(mainView, initialWidth, initialHeight);
            String cssUrl = getClass().getResource("/com/nhebpanha/cvstudio/styles.css") != null
                    ? getClass().getResource("/com/nhebpanha/cvstudio/styles.css").toExternalForm()
                    : null;
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl);
            }

            stage.setTitle("CV Studio — Nheb Panha — Computer Vision Pipeline (JavaFX 21 & OpenCV 4.9)");
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            stage.setMaximized(true);
            stage.show();

            // 7. Load default initial sample so all 4 panels display content immediately
            Platform.runLater(() -> loadSampleImage("Traffic & Vehicles"));

            // 8. Attempt background model check
            Task<Void> modelTask = new Task<>() {
                @Override
                protected Void call() {
                    Path modelPath = Path.of("models/yolov8n.onnx");
                    if (Files.exists(modelPath)) {
                        try {
                            models.loadModel(modelPath);
                            Platform.runLater(() -> mainView.setModelName(models.getModelName()));
                        } catch (Exception e) {
                            EventLog.warn("Could not load external model: " + e.getMessage());
                        }
                    } else {
                        EventLog.info("Using high-speed built-in detection engine.");
                    }
                    return null;
                }
            };
            Thread modelThread = new Thread(modelTask, "model-loader");
            modelThread.setDaemon(true);
            modelThread.start();
        }

        private void loadImageFile(File file) {
            if (file == null || !file.exists())
                return;
            EventLog.info("Loading image file: " + file.getName());

            Mat src = Imgcodecs.imread(file.getAbsolutePath());
            if (src.empty()) {
                EventLog.error("Unsupported or corrupted image file: " + file.getName());
                return;
            }

            runner.submit(new Frame(src));
        }

        private void loadSampleImage(String sampleName) {
            EventLog.info("Generating test scene: " + sampleName);
            Mat sample = SampleImages.createSample(sampleName);
            runner.submit(new Frame(sample));
        }

        @Override
        public void stop() {
            EventLog.info("Shutting down CV Studio...");
            if (camera != null) {
                camera.stopCamera();
            }
            if (runner != null) {
                runner.shutdown();
            }
            if (models != null) {
                models.close();
            }
        }
    }
}