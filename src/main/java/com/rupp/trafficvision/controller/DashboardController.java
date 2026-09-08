package com.rupp.trafficvision.controller;

import com.rupp.trafficvision.model.*;
import com.rupp.trafficvision.service.CameraService;
import com.rupp.trafficvision.service.ImageProcessingService;
import com.rupp.trafficvision.service.StatisticsService;
import com.rupp.trafficvision.service.VehicleDetectionService;
import com.rupp.trafficvision.sync.lock.ProcessingLock;
import com.rupp.trafficvision.sync.semaphore.ProcessingSemaphore;
import com.rupp.trafficvision.thread.ImageProcessingTask;
import com.rupp.trafficvision.thread.ProcessingWorker;
import com.rupp.trafficvision.ui.components.TrafficLightComponent;
import com.rupp.trafficvision.util.ImageUtil;
import com.rupp.trafficvision.util.LoggerUtil;
import com.rupp.trafficvision.util.TimeUtil;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Main presentation controller for the Traffic Light Vehicle Image Processing Dashboard.
 * Coordinates user interactions, asynchronous processing tasks, real-time camera simulation,
 * and thread-safe UI updates.
 */
public class DashboardController implements Initializable {

    // Top Navigation
    @FXML private Label lblLiveClock;
    @FXML private Label lblSystemStatus;
    @FXML private RadioButton radioLock;
    @FXML private RadioButton radioSemaphore;
    @FXML private ToggleGroup syncToggleGroup;
    @FXML private Button btnToggleCamera;

    // Left Control Panel
    @FXML private Button btnTakePicture;
    @FXML private Button btnImportImage;
    @FXML private ComboBox<String> comboSamples;
    @FXML private Button btnReset;
    @FXML private ComboBox<ProcessingParameters.Mode> comboProcessingMode;
    @FXML private CheckBox chkResize;
    @FXML private CheckBox chkGrayscale;
    @FXML private CheckBox chkNoiseReduction;
    @FXML private CheckBox chkEdgeDetection;
    @FXML private CheckBox chkContrast;
    @FXML private Slider sliderBrightness;
    @FXML private Label lblBrightnessVal;
    @FXML private Slider sliderContrast;
    @FXML private Label lblContrastVal;
    @FXML private Slider sliderThreshold;
    @FXML private Label lblThresholdVal;
    @FXML private Button btnProcessImage;

    // Center Viewport
    @FXML private StackPane viewportContainer;
    @FXML private VBox singleViewBox;
    @FXML private ImageView mainImageView;
    @FXML private Pane overlayPane;
    @FXML private HBox sideBySideBox;
    @FXML private ImageView splitOriginalView;
    @FXML private ImageView splitProcessedView;
    @FXML private VBox trafficLightContainer;
    @FXML private Button btnViewProcessed;
    @FXML private Button btnViewOriginal;
    @FXML private Button btnViewSideBySide;
    @FXML private Label lblProcessingStage;
    @FXML private Label lblProcessingPercentage;
    @FXML private ProgressBar progressBar;

    // Right Result & Analytics Panel
    @FXML private Label lblVehicleEmoji;
    @FXML private Label lblVehicleType;
    @FXML private Label lblConfidence;
    @FXML private Label lblProcessingTime;
    @FXML private Label lblDetectionStatus;
    @FXML private Button btnSaveResult;
    @FXML private Label lblCountCars;
    @FXML private Label lblCountMotorcycles;
    @FXML private Label lblCountBicycles;
    @FXML private Label lblCountTricycles;
    @FXML private Label lblCountMopeds;
    @FXML private LineChart<Number, Number> chartPerformance;
    @FXML private PieChart chartDistribution;

    // Bottom Tables
    @FXML private TableView<Vehicle> tableVehicleLog;
    @FXML private TableColumn<Vehicle, String> colLogImageId;
    @FXML private TableColumn<Vehicle, VehicleType> colLogType;
    @FXML private TableColumn<Vehicle, String> colLogConfidence;
    @FXML private TableColumn<Vehicle, String> colLogProcessingTime;
    @FXML private TableColumn<Vehicle, String> colLogThread;
    @FXML private TableColumn<Vehicle, String> colLogStatus;
    @FXML private TableColumn<Vehicle, String> colLogTimestamp;

    @FXML private TableView<ProcessingWorker> tableWorkerMonitor;
    @FXML private TableColumn<ProcessingWorker, String> colWorkerName;
    @FXML private TableColumn<ProcessingWorker, Number> colWorkerId;
    @FXML private TableColumn<ProcessingWorker, ThreadStatus> colWorkerStatus;
    @FXML private TableColumn<ProcessingWorker, String> colWorkerTask;
    @FXML private TableColumn<ProcessingWorker, String> colWorkerLastActivity;
    @FXML private TableColumn<ProcessingWorker, Number> colWorkerExecTime;

    // Services & Components
    private final StatisticsService statisticsService = new StatisticsService();
    private final VehicleDetectionService detectionService = new VehicleDetectionService(statisticsService);
    private final ImageProcessingService imageProcessingService = new ImageProcessingService();
    private final CameraService cameraService = new CameraService("CAM-01");
    private final TrafficLightComponent trafficLightComponent = new TrafficLightComponent();

    // Runtime State
    private BufferedImage currentOriginalImage;
    private BufferedImage currentProcessedImage;
    private DetectionResult latestDetectionResult;
    private Timeline clockTimeline;

    private final ObservableList<Vehicle> vehicleLogData = FXCollections.observableArrayList();
    private final XYChart.Series<Number, Number> performanceSeries = new XYChart.Series<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupClock();
        setupTrafficLight();
        setupControls();
        setupTables();
        setupCharts();
        setupDragAndDrop();
        setupServices();

        // Load initial synthetic sample
        loadSampleVehicle(VehicleType.CAR);
    }

    private void setupClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblLiveClock.setText(TimeUtil.formatCurrentTime());
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void setupTrafficLight() {
        trafficLightContainer.getChildren().add(trafficLightComponent);
        trafficLightComponent.setAutoCycle(true);
        trafficLightComponent.setOnColorChange(color -> {
            cameraService.setTrafficLightColor(color);
        });
    }

    private void setupControls() {
        // Mode dropdown
        comboProcessingMode.getItems().addAll(ProcessingParameters.Mode.values());
        comboProcessingMode.setValue(ProcessingParameters.Mode.VEHICLE_DETECTION);
        comboProcessingMode.setOnAction(e -> applyCurrentModePreview());

        // Quick samples dropdown
        comboSamples.getItems().addAll("🚗 Car", "🏍 Motorcycle", "🚲 Bicycle", "🛺 Tricycle", "🛵 Moped");
        comboSamples.setOnAction(e -> {
            String selected = comboSamples.getValue();
            if (selected != null) {
                if (selected.contains("Car")) loadSampleVehicle(VehicleType.CAR);
                else if (selected.contains("Motorcycle")) loadSampleVehicle(VehicleType.MOTORCYCLE);
                else if (selected.contains("Bicycle")) loadSampleVehicle(VehicleType.BICYCLE);
                else if (selected.contains("Tricycle")) loadSampleVehicle(VehicleType.TRICYCLE);
                else if (selected.contains("Moped")) loadSampleVehicle(VehicleType.MOPED);
            }
        });

        // Sliders
        sliderBrightness.valueProperty().addListener((obs, oldV, newV) -> {
            lblBrightnessVal.setText(String.format("%d", newV.intValue()));
            applyCurrentModePreview();
        });
        sliderContrast.valueProperty().addListener((obs, oldV, newV) -> {
            lblContrastVal.setText(String.format("%.1fx", newV.doubleValue()));
            applyCurrentModePreview();
        });
        sliderThreshold.valueProperty().addListener((obs, oldV, newV) -> {
            lblThresholdVal.setText(String.format("%d", newV.intValue()));
            applyCurrentModePreview();
        });

        // Filter Checkboxes
        chkResize.setOnAction(e -> applyCurrentModePreview());
        chkGrayscale.setOnAction(e -> applyCurrentModePreview());
        chkNoiseReduction.setOnAction(e -> applyCurrentModePreview());
        chkEdgeDetection.setOnAction(e -> applyCurrentModePreview());
        chkContrast.setOnAction(e -> applyCurrentModePreview());

        // Sync Strategy Radios
        radioLock.setOnAction(e -> {
            detectionService.setSynchronizationStrategy(new ProcessingLock());
            lblSystemStatus.setText("● MUTEX (REENTRANT LOCK)");
        });
        radioSemaphore.setOnAction(e -> {
            detectionService.setSynchronizationStrategy(new ProcessingSemaphore(2));
            lblSystemStatus.setText("● SEMAPHORE (CONCURRENCY: 2)");
        });
    }

    private void setupTables() {
        // Vehicle Log Table
        colLogImageId.setCellValueFactory(data -> data.getValue().vehicleIdProperty());
        colLogType.setCellValueFactory(data -> data.getValue().vehicleTypeProperty());
        colLogConfidence.setCellValueFactory(data -> data.getValue().confidenceProperty().asString("%.1f%%"));
        colLogProcessingTime.setCellValueFactory(data -> data.getValue().processingTimeProperty().asString("%d ms"));
        colLogThread.setCellValueFactory(data -> data.getValue().threadNameProperty());
        colLogStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colLogTimestamp.setCellValueFactory(data -> data.getValue().timestampProperty().asString());
        tableVehicleLog.setItems(vehicleLogData);

        // Worker Pool Monitor Table
        colWorkerName.setCellValueFactory(data -> data.getValue().threadNameProperty());
        colWorkerId.setCellValueFactory(data -> data.getValue().threadIdProperty());
        colWorkerStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colWorkerTask.setCellValueFactory(data -> data.getValue().currentTaskProperty());
        colWorkerLastActivity.setCellValueFactory(data -> data.getValue().lastActivityProperty().asString());
        colWorkerExecTime.setCellValueFactory(data -> data.getValue().executionTimeMsProperty());
        tableWorkerMonitor.setItems(FXCollections.observableArrayList(detectionService.getWorkerRegistry()));
    }

    private void setupCharts() {
        performanceSeries.setName("Processing Time (ms)");
        chartPerformance.getData().add(performanceSeries);

        updateDistributionChart();
    }

    private void setupDragAndDrop() {
        viewportContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != viewportContainer && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        viewportContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File file = db.getFiles().get(0);
                loadImageFile(file);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupServices() {
        statisticsService.addListener(this::refreshStatisticsUI);
    }

    // =========================================================================
    // IMAGE LOADING & SAMPLES
    // =========================================================================

    public void loadSampleVehicle(VehicleType type) {
        cameraService.setSimulatedVehicle(type);
        BufferedImage sample = cameraService.renderSyntheticFrame(type, 0.48, trafficLightComponent.getCurrentColor());
        setImageScene(sample, "Sample: " + type.getDisplayName());
    }

    private void setImageScene(BufferedImage img, String sourceDesc) {
        if (img == null) return;
        this.currentOriginalImage = img;
        this.currentProcessedImage = img;
        clearOverlays();

        Image fxImg = ImageUtil.toFXImage(img);
        mainImageView.setImage(fxImg);
        splitOriginalView.setImage(fxImg);
        splitProcessedView.setImage(fxImg);

        lblProcessingStage.setText("Loaded: " + sourceDesc);
        lblProcessingPercentage.setText("Ready");
        progressBar.setProgress(0.0);
    }

    private void loadImageFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Image", "Unable to decode image file:\n" + file.getName());
                return;
            }
            setImageScene(img, file.getName());
            LoggerUtil.info("Loaded image from file: " + file.getAbsolutePath());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "File Read Error", "Error reading image:\n" + e.getMessage());
        }
    }

    // =========================================================================
    // ACTIONS & HANDLERS
    // =========================================================================

    @FXML
    private void handleTakePicture() {
        BufferedImage snapshot = cameraService.captureSnapshot();
        setImageScene(snapshot, "Camera Snapshot (" + cameraService.getCameraId() + ")");
        lblSystemStatus.setText("● SNAPSHOT CAPTURED");
    }

    @FXML
    private void handleImportImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Traffic Vehicle Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selected = fileChooser.showOpenDialog(viewportContainer.getScene().getWindow());
        if (selected != null) {
            loadImageFile(selected);
        }
    }

    @FXML
    private void handleReset() {
        statisticsService.reset();
        vehicleLogData.clear();
        performanceSeries.getData().clear();
        clearOverlays();
        loadSampleVehicle(VehicleType.CAR);
        lblSystemStatus.setText("● SYSTEM RESET COMPLETED");
    }

    @FXML
    private void handleToggleCamera() {
        if (cameraService.isRunning()) {
            cameraService.stop();
            btnToggleCamera.setText("Start Live Camera");
            lblSystemStatus.setText("● SYSTEM READY");
        } else {
            btnToggleCamera.setText("Stop Live Camera");
            lblSystemStatus.setText("● LIVE CAMERA STREAMING");
            cameraService.start(frame -> {
                Platform.runLater(() -> {
                    currentOriginalImage = frame;
                    Image fxImg = ImageUtil.toFXImage(frame);
                    mainImageView.setImage(fxImg);
                    splitOriginalView.setImage(fxImg);
                });
            });
        }
    }

    @FXML
    private void handleProcessImage() {
        if (currentOriginalImage == null) {
            showAlert(Alert.AlertType.WARNING, "No Image", "Please import or take an image first.");
            return;
        }

        ProcessingParameters params = buildParameters();
        btnProcessImage.setDisable(true);
        lblSystemStatus.setText("● PROCESSING VEHICLE...");
        clearOverlays();

        // Submit to background thread worker
        ImageProcessingTask task = detectionService.submitDetectionTask(
                currentOriginalImage,
                params,
                previewImg -> Platform.runLater(() -> {
                    currentProcessedImage = previewImg;
                    Image fxImg = ImageUtil.toFXImage(previewImg);
                    mainImageView.setImage(fxImg);
                    splitProcessedView.setImage(fxImg);
                })
        );

        // Bind progress bar & stage labels
        progressBar.progressProperty().bind(task.progressProperty());
        task.messageProperty().addListener((obs, oldM, newM) -> {
            lblProcessingStage.setText(newM);
            double prog = task.getProgress();
            if (prog >= 0) {
                lblProcessingPercentage.setText(String.format("%.0f%%", prog * 100));
            }
        });

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            lblProcessingPercentage.setText("100%");
            btnProcessImage.setDisable(false);

            DetectionResult result = task.getValue();
            this.latestDetectionResult = result;
            renderDetectionResult(result);
            lblSystemStatus.setText("● DETECTION COMPLETE");

            // Add to TableView log
            Vehicle vehicleRecord = Vehicle.fromDetectionResult(result, Thread.currentThread().getName(), "Snapshot");
            vehicleLogData.add(0, vehicleRecord);

            // Animate bounding box
            renderBoundingBoxOverlay(result);
            tableWorkerMonitor.refresh();
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            btnProcessImage.setDisable(false);
            lblSystemStatus.setText("● PROCESSING FAILED");
            showAlert(Alert.AlertType.ERROR, "Processing Error", "Failed to process image: " + task.getException().getMessage());
            tableWorkerMonitor.refresh();
        });
    }

    private void applyCurrentModePreview() {
        if (currentOriginalImage == null) return;
        ProcessingParameters params = buildParameters();
        if (params.getMode() == ProcessingParameters.Mode.ORIGINAL) {
            currentProcessedImage = currentOriginalImage;
            Image fxImg = ImageUtil.toFXImage(currentOriginalImage);
            mainImageView.setImage(fxImg);
            splitProcessedView.setImage(fxImg);
            return;
        }

        BufferedImage filtered = imageProcessingService.process(currentOriginalImage, params);
        if (filtered != null) {
            currentProcessedImage = filtered;
            Image fxImg = ImageUtil.toFXImage(filtered);
            mainImageView.setImage(fxImg);
            splitProcessedView.setImage(fxImg);
        }
    }

    private ProcessingParameters buildParameters() {
        ProcessingParameters p = new ProcessingParameters();
        p.setMode(comboProcessingMode.getValue());
        p.setBrightness(sliderBrightness.getValue());
        p.setContrast(sliderContrast.getValue());
        p.setThreshold((int) sliderThreshold.getValue());
        p.setResize(chkResize.isSelected());
        p.setGrayscale(chkGrayscale.isSelected());
        p.setNoiseReduction(chkNoiseReduction.isSelected());
        p.setEdgeDetection(chkEdgeDetection.isSelected());
        p.setContrastEnhancement(chkContrast.isSelected());
        return p;
    }

    // =========================================================================
    // VIEWER MODES & OVERLAYS
    // =========================================================================

    @FXML
    private void handleShowProcessedView() {
        singleViewBox.setVisible(true);
        singleViewBox.setManaged(true);
        sideBySideBox.setVisible(false);
        sideBySideBox.setManaged(false);

        btnViewProcessed.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;");
        btnViewOriginal.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E5E7EB;");
        btnViewSideBySide.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E5E7EB;");

        if (currentProcessedImage != null) {
            mainImageView.setImage(ImageUtil.toFXImage(currentProcessedImage));
        }
    }

    @FXML
    private void handleShowOriginalView() {
        singleViewBox.setVisible(true);
        singleViewBox.setManaged(true);
        sideBySideBox.setVisible(false);
        sideBySideBox.setManaged(false);

        btnViewProcessed.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E5E7EB;");
        btnViewOriginal.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;");
        btnViewSideBySide.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E5E7EB;");

        if (currentOriginalImage != null) {
            mainImageView.setImage(ImageUtil.toFXImage(currentOriginalImage));
        }
    }

    @FXML
    private void handleShowSideBySideView() {
        singleViewBox.setVisible(false);
        singleViewBox.setManaged(false);
        sideBySideBox.setVisible(true);
        sideBySideBox.setManaged(true);

        btnViewProcessed.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E5E7EB;");
        btnViewOriginal.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E5E7EB;");
        btnViewSideBySide.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;");

        if (currentOriginalImage != null) {
            splitOriginalView.setImage(ImageUtil.toFXImage(currentOriginalImage));
        }
        if (currentProcessedImage != null) {
            splitProcessedView.setImage(ImageUtil.toFXImage(currentProcessedImage));
        }
    }

    private void renderBoundingBoxOverlay(DetectionResult result) {
        clearOverlays();
        if (result == null || !result.isDetected()) return;

        Image img = mainImageView.getImage();
        if (img == null) return;

        // Calculate aspect ratios & scaling of ImageView bounds
        double viewW = mainImageView.getBoundsInParent().getWidth();
        double viewH = mainImageView.getBoundsInParent().getHeight();
        double imgW = img.getWidth();
        double imgH = img.getHeight();

        double scaleX = viewW / imgW;
        double scaleY = viewH / imgH;

        BoundingBox box = result.getBoundingBox();
        double bx = mainImageView.getLayoutX() + box.getX() * scaleX;
        double by = mainImageView.getLayoutY() + box.getY() * scaleY;
        double bw = box.getWidth() * scaleX;
        double bh = box.getHeight() * scaleY;

        // Bounding Rectangle with glowing emerald borders
        Rectangle rect = new Rectangle(bx, by, bw, bh);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.web("#10B981"));
        rect.setStrokeWidth(3);
        rect.setArcWidth(8);
        rect.setArcHeight(8);
        rect.setEffect(new DropShadow(14, Color.web("#10B981")));

        // Vehicle Label Badge
        Label label = new Label(String.format("%s %s (%.1f%%)",
                result.getVehicleType().getEmoji(),
                result.getVehicleType().getDisplayName().toUpperCase(),
                result.getConfidencePercentage()));
        label.setStyle("""
                -fx-background-color: rgba(15, 23, 42, 0.9);
                -fx-text-fill: #FFFFFF;
                -fx-font-family: 'Segoe UI', sans-serif;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
                -fx-padding: 3 8 3 8;
                -fx-background-radius: 6;
                -fx-border-color: #10B981;
                -fx-border-radius: 6;
                """);
        label.setLayoutX(bx);
        label.setLayoutY(Math.max(4, by - 24));

        overlayPane.getChildren().addAll(rect, label);

        // Smooth fade-in animation
        FadeTransition ft = new FadeTransition(Duration.millis(350), overlayPane);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void clearOverlays() {
        overlayPane.getChildren().clear();
    }

    // =========================================================================
    // DETECTION RESULT HUD & STATISTICS
    // =========================================================================

    private void renderDetectionResult(DetectionResult result) {
        if (result == null) return;
        VehicleType type = result.getVehicleType();

        lblVehicleEmoji.setText(type.getEmoji());
        lblVehicleType.setText(type.name());
        lblConfidence.setText(String.format("%.1f%% Confidence", result.getConfidencePercentage()));
        lblProcessingTime.setText(String.format("Processing: %d ms", result.getProcessingTime()));

        if (result.isDetected()) {
            lblDetectionStatus.setText("✓ DETECTED");
            lblDetectionStatus.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-border-color: #10B981; -fx-text-fill: #34D399;");
        } else {
            lblDetectionStatus.setText("✗ UNKNOWN");
            lblDetectionStatus.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-border-color: #EF4444; -fx-text-fill: #F87171;");
        }

        // Scale & Pop Animation on the result card
        ScaleTransition st = new ScaleTransition(Duration.millis(250), lblVehicleEmoji);
        st.setFromX(0.7);
        st.setFromY(0.7);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    private void refreshStatisticsUI() {
        var mgr = statisticsService.getStatisticsManager();
        lblCountCars.setText(String.valueOf(mgr.getCarsDetected()));
        lblCountMotorcycles.setText(String.valueOf(mgr.getMotorcyclesDetected()));
        lblCountBicycles.setText(String.valueOf(mgr.getBicyclesDetected()));
        lblCountTricycles.setText(String.valueOf(mgr.getTricyclesDetected()));
        lblCountMopeds.setText(String.valueOf(mgr.getMopedsDetected()));

        // Add performance point
        int index = mgr.getProcessedImages();
        long lastTime = latestDetectionResult != null ? latestDetectionResult.getProcessingTime() : 0;
        performanceSeries.getData().add(new XYChart.Data<>(index, lastTime));
        if (performanceSeries.getData().size() > 25) {
            performanceSeries.getData().remove(0);
        }

        updateDistributionChart();
    }

    private void updateDistributionChart() {
        var mgr = statisticsService.getStatisticsManager();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Car (" + mgr.getCarsDetected() + ")", Math.max(1, mgr.getCarsDetected())),
                new PieChart.Data("Motorcycle (" + mgr.getMotorcyclesDetected() + ")", Math.max(0, mgr.getMotorcyclesDetected())),
                new PieChart.Data("Bicycle (" + mgr.getBicyclesDetected() + ")", Math.max(0, mgr.getBicyclesDetected())),
                new PieChart.Data("Tricycle (" + mgr.getTricyclesDetected() + ")", Math.max(0, mgr.getTricyclesDetected())),
                new PieChart.Data("Moped (" + mgr.getMopedsDetected() + ")", Math.max(0, mgr.getMopedsDetected()))
        );
        chartDistribution.setData(pieData);
    }

    // =========================================================================
    // SAVE RESULT
    // =========================================================================

    @FXML
    private void handleSaveResult() {
        if (currentOriginalImage == null) {
            showAlert(Alert.AlertType.WARNING, "Save Result", "No image available to save.");
            return;
        }

        BufferedImage annotated = ImageUtil.createAnnotatedImage(currentOriginalImage, latestDetectionResult);
        String filename = String.format("vehicle_detection_%s.png", TimeUtil.formatForFilename(LocalDateTime.now()));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Detection Result Image");
        fileChooser.setInitialFileName(filename);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Images (*.png)", "*.png"));

        File saveDest = fileChooser.showSaveDialog(viewportContainer.getScene().getWindow());
        if (saveDest != null) {
            try {
                ImageUtil.saveImage(annotated, saveDest);
                showAlert(Alert.AlertType.INFORMATION, "Save Successful",
                        "Annotated detection output successfully saved:\n" + saveDest.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Save Failed", "Failed to save output image: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shuts down all background services, executor threads, and camera streams on application exit.
     */
    public void cleanup() {
        if (clockTimeline != null) clockTimeline.stop();
        if (cameraService != null) cameraService.stop();
        if (detectionService != null) detectionService.shutdown();
        if (trafficLightComponent != null) trafficLightComponent.setAutoCycle(false);
        LoggerUtil.info("DashboardController resources cleaned up.");
    }
}
