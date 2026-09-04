package com.turbotech.cvstudio;

import com.turbotech.cvstudio.core.CameraService;
import com.turbotech.cvstudio.core.ModelService;
import com.turbotech.cvstudio.core.NativeLoader;
import com.turbotech.cvstudio.core.SampleImages;
import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;
import com.turbotech.cvstudio.pipeline.*;
import com.turbotech.cvstudio.ui.MainView;
import com.turbotech.cvstudio.util.EventLog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main application entry point for CV Studio.
 * Boots OpenCV native libraries, initializes the 5-stage vision pipeline, sets up UI,
 * and manages background lifecycle.
 */
public class App extends Application {

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
                mainView::update
        );

        // 4. Set up Camera Service
        camera = new CameraService(mat -> runner.submit(new Frame(mat)));
        mainView.bindCamera(camera);

        // 5. Wire action handlers
        mainView.setOpenImageHandler(this::loadImageFile);
        mainView.setSampleSelectedHandler(this::loadSampleImage);
        mainView.setRunHandler(() -> runner.reprocessCurrent());

        // 6. Assemble Scene
        Scene scene = new Scene(mainView, 1440, 900);
        String cssUrl = getClass().getResource("/com/turbotech/cvstudio/styles.css") != null
                ? getClass().getResource("/com/turbotech/cvstudio/styles.css").toExternalForm()
                : null;
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl);
        }

        stage.setTitle("CV Studio — Computer Vision Pipeline (JavaFX 21 & OpenCV 4.9)");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
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
        if (file == null || !file.exists()) return;
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

    public static void main(String[] args) {
        launch(args);
    }
}
