package com.turbotech.cvstudio.ui;

import com.turbotech.cvstudio.core.CameraService;
import com.turbotech.cvstudio.core.ImageUtils;
import com.turbotech.cvstudio.model.DecisionRule;
import com.turbotech.cvstudio.model.Detection;
import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;
import com.turbotech.cvstudio.util.EventLog;
import com.turbotech.cvstudio.util.ReportExporter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Root JavaFX BorderPane container holding the top menu and toolbar,
 * 4-panel central vision stage grid, right-side controls accordion,
 * bottom detection table / log viewer, and real-time status bar.
 */
public class MainView extends BorderPane {

    private final PipelineSettings settings;
    private final StagePanelView stage1View;
    private final StagePanelView stage2View;
    private final StagePanelView stage3View;
    private final StagePanelView stage4View;
    private final ControlsPane controlsPane;

    // Bottom components
    private final TableView<Detection> detectionsTable;
    private final ObservableList<Detection> detectionsList = FXCollections.observableArrayList();
    private final ListView<EventLog.LogEntry> logListView;
    private final Label decisionBadge;

    // Status bar metrics
    private final Label statusResolution;
    private final Label statusFps;
    private final Label statusLatency;
    private final Label statusModel;
    private final Label statusDecision;

    // FPS calculation state
    private long lastFrameTimeNano = 0;
    private double smoothedFps = 0.0;
    private Frame lastFrame = null;

    // Buffer reuse for high throughput
    private WritableImage stage1Image;
    private WritableImage stage2Image;
    private WritableImage stage3Image;
    private WritableImage stage4Image;

    // Handlers
    private Consumer<File> openImageHandler;
    private Consumer<String> sampleSelectedHandler;
    private Runnable runHandler;
    private Runnable pauseHandler;

    public MainView(PipelineSettings settings, Runnable onSettingsChanged) {
        this.settings = settings;

        getStyleClass().add("main-root");

        // 1. Stage panels
        stage1View = new StagePanelView("1", "Image Acquisition (Source)", false);
        stage2View = new StagePanelView("2", "Preprocessing (Filtered)", false);
        stage3View = new StagePanelView("3", "Feature Extraction", false);
        stage4View = new StagePanelView("4", "Recognition & Classification", true);

        // 2. Controls accordion
        controlsPane = new ControlsPane(settings, onSettingsChanged);

        // 3. Top Menu & Toolbar
        VBox topBox = new VBox();
        topBox.getChildren().addAll(createMenuBar(), createToolBar());
        setTop(topBox);

        // 4. Center 2x2 Grid of Stage Panels
        GridPane grid = new GridPane();
        grid.getStyleClass().add("stage-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        row1.setVgrow(Priority.ALWAYS);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        row2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(row1, row2);

        grid.add(stage1View, 0, 0);
        grid.add(stage2View, 1, 0);
        grid.add(stage3View, 0, 1);
        grid.add(stage4View, 1, 1);

        setCenter(grid);
        setRight(controlsPane);

        // 5. Bottom Split: Detections Table + Event Log + Status Bar
        detectionsTable = createDetectionsTable();
        logListView = createLogListView();

        TabPane bottomTabs = new TabPane();
        bottomTabs.setPrefHeight(170);

        Tab tableTab = new Tab("Detections (" + detectionsList.size() + ")", detectionsTable);
        tableTab.setClosable(false);
        detectionsList.addListener((javafx.beans.Observable o) ->
                tableTab.setText("Detections (" + detectionsList.size() + ")")
        );

        Tab logTab = new Tab("Event Log", logListView);
        logTab.setClosable(false);

        bottomTabs.getTabs().addAll(tableTab, logTab);

        decisionBadge = new Label("No Decision");
        decisionBadge.getStyleClass().addAll("badge", "badge-decision");

        HBox bottomPanel = new HBox(10, bottomTabs);
        HBox.setHgrow(bottomTabs, Priority.ALWAYS);
        bottomPanel.setPadding(new Insets(0, 10, 5, 10));

        // Status Bar
        HBox statusBar = new HBox(16);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(6, 14, 6, 14));

        statusResolution = new Label("Resolution: —");
        statusFps = new Label("FPS: —");
        statusLatency = new Label("Pipeline: 0 ms");
        statusModel = new Label("Model: Built-in Detector");
        statusDecision = new Label("Decision: Normal");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(
                statusResolution,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                statusFps,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                statusLatency,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                statusModel,
                spacer,
                decisionBadge
        );

        VBox bottomContainer = new VBox(bottomPanel, statusBar);
        setBottom(bottomContainer);

        // Wire controls pane callbacks
        controlsPane.setImageFileSelectedHandler(f -> {
            if (openImageHandler != null) openImageHandler.accept(f);
        });
        controlsPane.setSampleSelectedHandler(s -> {
            if (sampleSelectedHandler != null) sampleSelectedHandler.accept(s);
        });
        controlsPane.setSnapshotAction(this::exportCurrentSnapshot);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Image...");
        openItem.setOnAction(e -> promptOpenImage());
        MenuItem exportReportItem = new MenuItem("Export Report (CSV + Snapshot)...");
        exportReportItem.setOnAction(e -> exportFullReport());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(openItem, exportReportItem, new SeparatorMenuItem(), exitItem);

        Menu pipelineMenu = new Menu("Pipeline");
        MenuItem runItem = new MenuItem("Re-run Pipeline");
        runItem.setOnAction(e -> {
            if (runHandler != null) runHandler.run();
        });
        pipelineMenu.getItems().add(runItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About CV Studio");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, pipelineMenu, helpMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("app-toolbar");

        Button openBtn = new Button("Open Image");
        openBtn.getStyleClass().add("toolbar-button");
        openBtn.setOnAction(e -> promptOpenImage());

        Button snapBtn = new Button("Snapshot");
        snapBtn.getStyleClass().add("toolbar-button");
        snapBtn.setOnAction(e -> exportCurrentSnapshot());

        Button runBtn = new Button("▶ Run");
        runBtn.getStyleClass().addAll("toolbar-button", "button-primary");
        runBtn.setOnAction(e -> {
            if (runHandler != null) runHandler.run();
        });

        Button reportBtn = new Button("Export Report");
        reportBtn.getStyleClass().add("toolbar-button");
        reportBtn.setOnAction(e -> exportFullReport());

        Button clearLogBtn = new Button("Clear Log");
        clearLogBtn.getStyleClass().add("toolbar-button");
        clearLogBtn.setOnAction(e -> EventLog.clear());

        toolBar.getItems().addAll(
                openBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                runBtn,
                snapBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                reportBtn,
                clearLogBtn
        );

        return toolBar;
    }

    private TableView<Detection> createDetectionsTable() {
        TableView<Detection> table = new TableView<>(detectionsList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Detection, String> labelCol = new TableColumn<>("Object Label");
        labelCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().label()));

        TableColumn<Detection, String> confCol = new TableColumn<>("Confidence");
        confCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().formattedConfidence()));

        TableColumn<Detection, String> boxCol = new TableColumn<>("Bounding Box [X, Y, W, H]");
        boxCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().formattedBox()));

        table.getColumns().addAll(labelCol, confCol, boxCol);
        return table;
    }

    private ListView<EventLog.LogEntry> createLogListView() {
        ListView<EventLog.LogEntry> listView = new ListView<>(EventLog.getEntries());
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(EventLog.LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: " + item.level().getColorHex() + "; -fx-font-family: 'Consolas', 'Courier New', monospace;");
                }
            }
        });
        return listView;
    }

    /**
     * Updates the UI with a completed pipeline Frame. Called on the JavaFX Application Thread.
     */
    public void update(Frame frame) {
        if (frame == null) return;
        this.lastFrame = frame;

        // 1. Calculate FPS
        long now = System.nanoTime();
        if (lastFrameTimeNano > 0) {
            double currentFps = 1_000_000_000.0 / (now - lastFrameTimeNano);
            smoothedFps = (smoothedFps == 0.0) ? currentFps : (smoothedFps * 0.85 + currentFps * 0.15);
            statusFps.setText(String.format("FPS: %.1f", smoothedFps));
        }
        lastFrameTimeNano = now;

        // 2. Stage 1: Original Image
        if (frame.getOriginal() != null && !frame.getOriginal().empty()) {
            stage1Image = ImageUtils.toFxImage(frame.getOriginal(), stage1Image);
            stage1View.update(stage1Image, frame.getOriginal().cols(), frame.getOriginal().rows());
            statusResolution.setText(frame.getOriginal().cols() + "x" + frame.getOriginal().rows());
        }

        // 3. Stage 2: Processed Image
        if (frame.getProcessed() != null && !frame.getProcessed().empty()) {
            stage2Image = ImageUtils.toFxImage(frame.getProcessed(), stage2Image);
            stage2View.update(stage2Image, frame.getProcessed().cols(), frame.getProcessed().rows());
        }

        // 4. Stage 3: Feature View
        if (frame.getFeatureView() != null && !frame.getFeatureView().empty()) {
            stage3Image = ImageUtils.toFxImage(frame.getFeatureView(), stage3Image);
            stage3View.update(stage3Image, frame.getFeatureView().cols(), frame.getFeatureView().rows());
        }

        // 5. Stage 4: Recognition & Detection Overlay
        org.opencv.core.Mat recMat = (frame.getProcessed() != null && !frame.getProcessed().empty())
                ? frame.getProcessed() : frame.getOriginal();
        if (recMat != null && !recMat.empty()) {
            stage4Image = ImageUtils.toFxImage(recMat, stage4Image);
            stage4View.update(stage4Image, recMat.cols(), recMat.rows());

            // Draw bounding boxes on canvas
            DetectionOverlay overlay = stage4View.getOverlay();
            if (overlay != null) {
                overlay.render(frame.getDetections(),
                        stage4View.getImageView().getBoundsInParent().getWidth(),
                        stage4View.getImageView().getBoundsInParent().getHeight(),
                        recMat.cols(), recMat.rows());
            }
        }

        // 6. Stage Latencies
        for (Frame.StageTiming timing : frame.getTimings()) {
            if (timing.stage().contains("1")) stage1View.setLatency(timing.millis());
            else if (timing.stage().contains("2")) stage2View.setLatency(timing.millis());
            else if (timing.stage().contains("3")) stage3View.setLatency(timing.millis());
            else if (timing.stage().contains("4")) stage4View.setLatency(timing.millis());
        }
        statusLatency.setText("Pipeline: " + frame.getTotalLatencyMs() + " ms");

        // 7. Update detections table
        detectionsList.setAll(frame.getDetections());

        // 8. Stage 5 Decision & Status
        String decision = frame.getDecision();
        decisionBadge.setText(decision);
        if (decision.contains("Alert") || decision.contains("ALERT")) {
            decisionBadge.setStyle("-fx-background-color: #ef5350; -fx-text-fill: white;");
        } else if (decision.contains("Normal")) {
            decisionBadge.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        } else {
            decisionBadge.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        }
    }

    private void promptOpenImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Image for CV Studio");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null && openImageHandler != null) {
            openImageHandler.accept(file);
        }
    }

    private void exportCurrentSnapshot() {
        if (lastFrame == null) {
            EventLog.warn("No frame available to snapshot.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Frame Snapshot");
        chooser.setInitialFileName("snapshot_" + System.currentTimeMillis() + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            ReportExporter.exportAnnotatedImage(lastFrame, file);
            EventLog.success("Snapshot exported to: " + file.getName());
        }
    }

    private void exportFullReport() {
        if (lastFrame == null) {
            EventLog.warn("No frame available to export report.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report (CSV)");
        chooser.setInitialFileName("cv_studio_report_" + System.currentTimeMillis() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Table", "*.csv"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                ReportExporter.exportCsv(lastFrame, file);
                EventLog.success("Inspection report exported to: " + file.getName());
            } catch (Exception ex) {
                EventLog.error("Export report failed: " + ex.getMessage());
            }
        }
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About CV Studio");
        alert.setHeaderText("CV Studio — Computer Vision Process");
        alert.setContentText("A modern JavaFX 21 desktop application demonstrating all 5 stages of the Computer Vision Process:\n\n"
                + "1. Image Acquisition\n"
                + "2. Preprocessing\n"
                + "3. Feature Extraction\n"
                + "4. Object Recognition & Classification\n"
                + "5. Decision Making\n\n"
                + "Built with JavaFX 21, OpenCV 4.9.0-0, and Deep Learning Inference.");
        alert.showAndWait();
    }

    public void bindCamera(CameraService cameraService) {
        controlsPane.bindCamera(cameraService);
    }

    public void setOpenImageHandler(Consumer<File> handler) {
        this.openImageHandler = handler;
    }

    public void setSampleSelectedHandler(Consumer<String> handler) {
        this.sampleSelectedHandler = handler;
    }

    public void setRunHandler(Runnable handler) {
        this.runHandler = handler;
    }

    public void setPauseHandler(Runnable handler) {
        this.pauseHandler = handler;
    }

    public void setModelName(String name) {
        statusModel.setText("Model: " + name);
    }
}
