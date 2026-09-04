package com.turbotech.cvstudio.ui;

import com.turbotech.cvstudio.core.CameraService;
import com.turbotech.cvstudio.core.ModelService;
import com.turbotech.cvstudio.model.DecisionRule;
import com.turbotech.cvstudio.model.PipelineSettings;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Accordion controls pane holding parameter controls for all 5 stages of the pipeline.
 * Implements slider debouncing via PauseTransition to prevent overloading the pipeline thread.
 */
public class ControlsPane extends VBox {

    private final PipelineSettings settings;
    private final Runnable onSettingsChanged;
    private final PauseTransition debounceTimer;

    private Consumer<File> imageFileSelectedHandler;
    private Consumer<String> sampleSelectedHandler;
    private Runnable snapshotAction;
    private CameraService cameraService;

    // Controls
    private Button startStopCamBtn;
    private Label camStatusLabel;

    public ControlsPane(PipelineSettings settings, Runnable onSettingsChanged) {
        this.settings = settings;
        this.onSettingsChanged = onSettingsChanged;

        // 120 ms debounce timer for slider drags
        this.debounceTimer = new PauseTransition(Duration.millis(120));
        this.debounceTimer.setOnFinished(e -> {
            if (onSettingsChanged != null) {
                onSettingsChanged.run();
            }
        });

        getStyleClass().add("controls-pane");
        setPadding(new Insets(10));
        setSpacing(10);
        setPrefWidth(340);
        setMinWidth(300);

        Label header = new Label("PIPELINE CONTROLS");
        header.getStyleClass().add("controls-header");

        Accordion accordion = new Accordion();
        VBox.setVgrow(accordion, Priority.ALWAYS);

        TitledPane pane1 = createAcquisitionPane();
        TitledPane pane2 = createPreprocessingPane();
        TitledPane pane3 = createFeatureExtractionPane();
        TitledPane pane4 = createRecognitionPane();
        TitledPane pane5 = createDecisionRulesPane();

        accordion.getPanes().addAll(pane1, pane2, pane3, pane4, pane5);
        accordion.setExpandedPane(pane2); // Expand Preprocessing by default

        getChildren().addAll(header, accordion);
    }

    private void triggerDebouncedChange() {
        debounceTimer.playFromStart();
    }

    // ==========================================
    // STAGE 1: ACQUISITION CONTROLS
    // ==========================================
    private TitledPane createAcquisitionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label sourceLabel = new Label("Image Input Source:");
        sourceLabel.getStyleClass().add("field-label");

        HBox fileBox = new HBox(8);
        Button openFileBtn = new Button("Choose File...");
        openFileBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(openFileBtn, Priority.ALWAYS);
        openFileBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Input Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp")
            );
            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null && imageFileSelectedHandler != null) {
                imageFileSelectedHandler.accept(file);
            }
        });
        fileBox.getChildren().add(openFileBtn);

        Label sampleLabel = new Label("Built-in Test Samples:");
        sampleLabel.getStyleClass().add("field-label");
        ComboBox<String> sampleCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Select Sample...", "Traffic & Vehicles", "Pedestrians & City", "Office & Desk", "Objects & Bottles"
        ));
        sampleCombo.setValue("Select Sample...");
        sampleCombo.setMaxWidth(Double.MAX_VALUE);
        sampleCombo.setOnAction(e -> {
            String val = sampleCombo.getValue();
            if (val != null && !val.startsWith("Select") && sampleSelectedHandler != null) {
                sampleSelectedHandler.accept(val);
            }
        });

        Separator sep = new Separator();

        Label camLabel = new Label("Live Camera Video:");
        camLabel.getStyleClass().add("field-label");

        HBox camRow = new HBox(8);
        camRow.setAlignment(Pos.CENTER_LEFT);
        Label devLabel = new Label("Device #:");
        Spinner<Integer> deviceSpinner = new Spinner<>(0, 5, 0);
        deviceSpinner.setPrefWidth(70);
        camRow.getChildren().addAll(devLabel, deviceSpinner);

        startStopCamBtn = new Button("Start Camera");
        startStopCamBtn.getStyleClass().add("button-primary");
        startStopCamBtn.setMaxWidth(Double.MAX_VALUE);
        startStopCamBtn.setOnAction(e -> toggleCamera(deviceSpinner.getValue()));

        camStatusLabel = new Label("Camera: Idle");
        camStatusLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 11px;");

        Button snapBtn = new Button("Take Snapshot");
        snapBtn.setMaxWidth(Double.MAX_VALUE);
        snapBtn.setOnAction(e -> {
            if (snapshotAction != null) snapshotAction.run();
        });

        content.getChildren().addAll(
                sourceLabel, fileBox, sampleLabel, sampleCombo,
                sep, camLabel, camRow, startStopCamBtn, camStatusLabel, snapBtn
        );

        return new TitledPane("1. Acquisition", content);
    }

    private void toggleCamera(int deviceIndex) {
        if (cameraService == null) return;

        if (cameraService.isStreaming()) {
            cameraService.stopCamera();
            startStopCamBtn.setText("Start Camera");
            startStopCamBtn.getStyleClass().remove("button-danger");
            startStopCamBtn.getStyleClass().add("button-primary");
            camStatusLabel.setText("Camera: Stopped");
        } else {
            cameraService.setDeviceIndex(deviceIndex);
            cameraService.restart();
            startStopCamBtn.setText("Stop Camera");
            startStopCamBtn.getStyleClass().remove("button-primary");
            startStopCamBtn.getStyleClass().add("button-danger");
            camStatusLabel.setText("Camera: Streaming device #" + deviceIndex);
        }
    }

    // ==========================================
    // STAGE 2: PREPROCESSING CONTROLS
    // ==========================================
    private TitledPane createPreprocessingPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Brightness
        Label bVal = new Label(String.format("%.0f", settings.getBrightness()));
        Slider bSlider = new Slider(-100, 100, settings.getBrightness());
        bSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setBrightness(n.doubleValue());
            bVal.setText(String.format("%.0f", n.doubleValue()));
            triggerDebouncedChange();
        });
        HBox bHeader = new HBox(8, new Label("Brightness:"), bVal);

        // Contrast
        Label cVal = new Label(String.format("%.2f", settings.getContrast()));
        Slider cSlider = new Slider(0.5, 3.0, settings.getContrast());
        cSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setContrast(n.doubleValue());
            cVal.setText(String.format("%.2f", n.doubleValue()));
            triggerDebouncedChange();
        });
        HBox cHeader = new HBox(8, new Label("Contrast:"), cVal);

        // Blur Kernel (odd)
        Label blurVal = new Label(String.valueOf(settings.getBlurKernel()));
        Slider blurSlider = new Slider(1, 15, settings.getBlurKernel());
        blurSlider.setBlockIncrement(2);
        blurSlider.valueProperty().addListener((obs, o, n) -> {
            int val = n.intValue();
            if (val > 1 && val % 2 == 0) val++;
            settings.setBlurKernel(val);
            blurVal.setText(String.valueOf(val));
            triggerDebouncedChange();
        });
        HBox blurHeader = new HBox(8, new Label("Denoise (Blur Kernel):"), blurVal);

        // Grayscale
        CheckBox grayCheck = new CheckBox("Convert to Grayscale");
        grayCheck.setSelected(settings.isGrayscale());
        grayCheck.selectedProperty().addListener((obs, o, n) -> {
            settings.setGrayscale(n);
            triggerDebouncedChange();
        });

        // CLAHE
        CheckBox claheCheck = new CheckBox("Enable CLAHE Equalization");
        claheCheck.setSelected(settings.isClaheEnabled());
        claheCheck.selectedProperty().addListener((obs, o, n) -> {
            settings.setClaheEnabled(n);
            triggerDebouncedChange();
        });

        Label claheVal = new Label(String.format("%.1f", settings.getClaheClipLimit()));
        Slider claheSlider = new Slider(1.0, 8.0, settings.getClaheClipLimit());
        claheSlider.disableProperty().bind(claheCheck.selectedProperty().not());
        claheSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setClaheClipLimit(n.doubleValue());
            claheVal.setText(String.format("%.1f", n.doubleValue()));
            triggerDebouncedChange();
        });
        HBox claheHeader = new HBox(8, new Label("CLAHE Clip Limit:"), claheVal);

        Button resetBtn = new Button("Reset Preprocessing");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> {
            bSlider.setValue(0);
            cSlider.setValue(1.0);
            blurSlider.setValue(3);
            grayCheck.setSelected(false);
            claheCheck.setSelected(false);
            claheSlider.setValue(2.0);
        });

        content.getChildren().addAll(
                bHeader, bSlider,
                cHeader, cSlider,
                blurHeader, blurSlider,
                grayCheck,
                claheCheck, claheHeader, claheSlider,
                resetBtn
        );

        return new TitledPane("2. Preprocessing", content);
    }

    // ==========================================
    // STAGE 3: FEATURE EXTRACTION CONTROLS
    // ==========================================
    private TitledPane createFeatureExtractionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label modeLabel = new Label("Feature Extraction Mode:");
        modeLabel.getStyleClass().add("field-label");

        ComboBox<PipelineSettings.FeatureMode> modeCombo = new ComboBox<>(
                FXCollections.observableArrayList(PipelineSettings.FeatureMode.values())
        );
        modeCombo.setValue(settings.getFeatureMode());
        modeCombo.setMaxWidth(Double.MAX_VALUE);
        modeCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                settings.setFeatureMode(n);
                triggerDebouncedChange();
            }
        });

        // Canny Low
        Label lowVal = new Label(String.format("%.0f", settings.getCannyLow()));
        Slider lowSlider = new Slider(10, 200, settings.getCannyLow());
        lowSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setCannyLow(n.doubleValue());
            lowVal.setText(String.format("%.0f", n.doubleValue()));
            triggerDebouncedChange();
        });
        HBox lowHeader = new HBox(8, new Label("Canny Low Threshold:"), lowVal);

        // Canny High
        Label highVal = new Label(String.format("%.0f", settings.getCannyHigh()));
        Slider highSlider = new Slider(50, 300, settings.getCannyHigh());
        highSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setCannyHigh(n.doubleValue());
            highVal.setText(String.format("%.0f", n.doubleValue()));
            triggerDebouncedChange();
        });
        HBox highHeader = new HBox(8, new Label("Canny High Threshold:"), highVal);

        // Min Contour Area
        Label areaVal = new Label(String.format("%.0f px", settings.getMinContourArea()));
        Slider areaSlider = new Slider(20, 2000, settings.getMinContourArea());
        areaSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setMinContourArea(n.doubleValue());
            areaVal.setText(String.format("%.0f px", n.doubleValue()));
            triggerDebouncedChange();
        });
        HBox areaHeader = new HBox(8, new Label("Min Contour Area:"), areaVal);

        content.getChildren().addAll(
                modeLabel, modeCombo,
                lowHeader, lowSlider,
                highHeader, highSlider,
                areaHeader, areaSlider
        );

        return new TitledPane("3. Feature Extraction", content);
    }

    // ==========================================
    // STAGE 4: RECOGNITION CONTROLS
    // ==========================================
    private TitledPane createRecognitionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        CheckBox inferToggle = new CheckBox("Enable Object Detection");
        inferToggle.setSelected(settings.isInferenceEnabled());
        inferToggle.selectedProperty().addListener((obs, o, n) -> {
            settings.setInferenceEnabled(n);
            triggerDebouncedChange();
        });

        Label confVal = new Label(String.format("%.0f%%", settings.getConfidenceThreshold() * 100));
        Slider confSlider = new Slider(0.15, 0.90, settings.getConfidenceThreshold());
        confSlider.valueProperty().addListener((obs, o, n) -> {
            settings.setConfidenceThreshold(n.doubleValue());
            confVal.setText(String.format("%.0f%%", n.doubleValue() * 100));
            triggerDebouncedChange();
        });
        HBox confHeader = new HBox(8, new Label("Min Confidence Threshold:"), confVal);

        Label filterLabel = new Label("Class Filter Preset:");
        filterLabel.getStyleClass().add("field-label");

        ComboBox<String> classPresetCombo = new ComboBox<>(FXCollections.observableArrayList(
                "All Classes (No Filter)", "People & Vehicles", "People Only", "Vehicles Only", "Indoor Objects"
        ));
        classPresetCombo.setValue("All Classes (No Filter)");
        classPresetCombo.setMaxWidth(Double.MAX_VALUE);
        classPresetCombo.setOnAction(e -> {
            settings.getClassFilter().clear();
            String sel = classPresetCombo.getValue();
            if ("People & Vehicles".equals(sel)) {
                settings.getClassFilter().addAll(java.util.List.of("person", "car", "bicycle", "motorcycle", "bus", "truck"));
            } else if ("People Only".equals(sel)) {
                settings.getClassFilter().add("person");
            } else if ("Vehicles Only".equals(sel)) {
                settings.getClassFilter().addAll(java.util.List.of("car", "bicycle", "motorcycle", "bus", "truck"));
            } else if ("Indoor Objects".equals(sel)) {
                settings.getClassFilter().addAll(java.util.List.of("bottle", "cup", "chair", "couch", "laptop", "cell phone"));
            }
            triggerDebouncedChange();
        });

        content.getChildren().addAll(inferToggle, confHeader, confSlider, filterLabel, classPresetCombo);
        return new TitledPane("4. Recognition", content);
    }

    // ==========================================
    // STAGE 5: DECISION RULES CONTROLS
    // ==========================================
    private TitledPane createDecisionRulesPane() {
        VBox content = new VBox(8);
        content.setPadding(new Insets(8));

        Label infoLabel = new Label("Active Business Decision Rules:");
        infoLabel.getStyleClass().add("field-label");

        TableView<DecisionRule> rulesTable = new TableView<>(settings.getRules());
        rulesTable.setPrefHeight(160);
        rulesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DecisionRule, String> nameCol = new TableColumn<>("Rule");
        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));

        TableColumn<DecisionRule, String> classCol = new TableColumn<>("Target");
        classCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().targetClass()));

        TableColumn<DecisionRule, Integer> countCol = new TableColumn<>("Min");
        countCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().minCount()));
        countCol.setPrefWidth(45);

        TableColumn<DecisionRule, String> actCol = new TableColumn<>("Action");
        actCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().action().name()));

        rulesTable.getColumns().addAll(nameCol, classCol, countCol, actCol);

        HBox btnRow = new HBox(8);
        Button addRuleBtn = new Button("+ Add Rule");
        addRuleBtn.setOnAction(e -> showAddRuleDialog());

        Button removeRuleBtn = new Button("Delete");
        removeRuleBtn.setOnAction(e -> {
            DecisionRule selected = rulesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                settings.getRules().remove(selected);
                triggerDebouncedChange();
            }
        });

        btnRow.getChildren().addAll(addRuleBtn, removeRuleBtn);

        content.getChildren().addAll(infoLabel, rulesTable, btnRow);
        return new TitledPane("5. Decision Rules", content);
    }

    private void showAddRuleDialog() {
        Dialog<DecisionRule> dialog = new Dialog<>();
        dialog.setTitle("Add Decision Rule");
        dialog.setHeaderText("Create a new rule triggered by object detections");

        ButtonType saveBtnType = new ButtonType("Add Rule", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        TextField nameField = new TextField("New Rule");
        TextField classField = new TextField("person");
        Spinner<Integer> countSpinner = new Spinner<>(1, 50, 1);
        Slider confSlider = new Slider(0.2, 0.95, 0.45);
        ComboBox<DecisionRule.Action> actionCombo = new ComboBox<>(
                FXCollections.observableArrayList(DecisionRule.Action.values())
        );
        actionCombo.setValue(DecisionRule.Action.ALERT);

        grid.add(new Label("Rule Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Target Class:"), 0, 1);
        grid.add(classField, 1, 1);
        grid.add(new Label("Min Count:"), 0, 2);
        grid.add(countSpinner, 1, 2);
        grid.add(new Label("Min Conf:"), 0, 3);
        grid.add(confSlider, 1, 3);
        grid.add(new Label("Trigger Action:"), 0, 4);
        grid.add(actionCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                return new DecisionRule(
                        nameField.getText().trim(),
                        classField.getText().trim().toLowerCase(),
                        countSpinner.getValue(),
                        confSlider.getValue(),
                        actionCombo.getValue()
                );
            }
            return null;
        });

        Optional<DecisionRule> result = dialog.showAndWait();
        result.ifPresent(r -> {
            settings.getRules().add(r);
            triggerDebouncedChange();
        });
    }

    public void bindCamera(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    public void setImageFileSelectedHandler(Consumer<File> handler) {
        this.imageFileSelectedHandler = handler;
    }

    public void setSampleSelectedHandler(Consumer<String> handler) {
        this.sampleSelectedHandler = handler;
    }

    public void setSnapshotAction(Runnable snapshotAction) {
        this.snapshotAction = snapshotAction;
    }
}
