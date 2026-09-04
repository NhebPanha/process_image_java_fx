package com.nhebpanha.cvstudio.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.List;

/**
 * Observable configuration model holding all tunable parameters across the pipeline.
 */
public class PipelineSettings {

    public enum FeatureMode {
        EDGES("Edge Detection (Canny)"),
        CONTOURS("Contour Detection"),
        KEYPOINTS("Feature Keypoints (ORB)"),
        HISTOGRAM("Color / Intensity Histogram");

        private final String displayName;

        FeatureMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Preprocessing properties
    private final DoubleProperty brightness = new SimpleDoubleProperty(0.0);
    private final DoubleProperty contrast = new SimpleDoubleProperty(1.0);
    private final IntegerProperty blurKernel = new SimpleIntegerProperty(3);
    private final BooleanProperty grayscale = new SimpleBooleanProperty(false);
    private final BooleanProperty claheEnabled = new SimpleBooleanProperty(false);
    private final DoubleProperty claheClipLimit = new SimpleDoubleProperty(2.0);
    private final BooleanProperty resizeEnabled = new SimpleBooleanProperty(false);
    private final IntegerProperty targetWidth = new SimpleIntegerProperty(640);
    private final IntegerProperty targetHeight = new SimpleIntegerProperty(480);

    // Feature extraction properties
    private final ObjectProperty<FeatureMode> featureMode = new SimpleObjectProperty<>(FeatureMode.EDGES);
    private final DoubleProperty cannyLow = new SimpleDoubleProperty(50.0);
    private final DoubleProperty cannyHigh = new SimpleDoubleProperty(150.0);
    private final DoubleProperty minContourArea = new SimpleDoubleProperty(150.0);

    // Recognition properties
    private final DoubleProperty confidenceThreshold = new SimpleDoubleProperty(0.35);
    private final BooleanProperty inferenceEnabled = new SimpleBooleanProperty(true);
    private final ObservableSet<String> classFilter = FXCollections.observableSet();

    // Decision stage properties
    private final ObservableList<DecisionRule> rules = FXCollections.observableArrayList();

    public PipelineSettings() {
        // Populate standard default decision rules as specified in readme.md
        rules.addAll(List.of(
                new DecisionRule("Person Alert", "person", 1, 0.40, DecisionRule.Action.ALERT),
                new DecisionRule("Vehicle Counting", "car", 1, 0.40, DecisionRule.Action.LOG),
                new DecisionRule("Object Highlight", "dog", 1, 0.40, DecisionRule.Action.HIGHLIGHT),
                new DecisionRule("Bottle Inspection", "bottle", 1, 0.35, DecisionRule.Action.SNAPSHOT)
        ));
    }

    // Getters & Property accessors
    public double getBrightness() { return brightness.get(); }
    public void setBrightness(double v) { brightness.set(v); }
    public DoubleProperty brightnessProperty() { return brightness; }

    public double getContrast() { return contrast.get(); }
    public void setContrast(double v) { contrast.set(v); }
    public DoubleProperty contrastProperty() { return contrast; }

    public int getBlurKernel() { return blurKernel.get(); }
    public void setBlurKernel(int v) { blurKernel.set(v); }
    public IntegerProperty blurKernelProperty() { return blurKernel; }

    public boolean isGrayscale() { return grayscale.get(); }
    public void setGrayscale(boolean v) { grayscale.set(v); }
    public BooleanProperty grayscaleProperty() { return grayscale; }

    public boolean isClaheEnabled() { return claheEnabled.get(); }
    public void setClaheEnabled(boolean v) { claheEnabled.set(v); }
    public BooleanProperty claheEnabledProperty() { return claheEnabled; }

    public double getClaheClipLimit() { return claheClipLimit.get(); }
    public void setClaheClipLimit(double v) { claheClipLimit.set(v); }
    public DoubleProperty claheClipLimitProperty() { return claheClipLimit; }

    public boolean isResizeEnabled() { return resizeEnabled.get(); }
    public void setResizeEnabled(boolean v) { resizeEnabled.set(v); }
    public BooleanProperty resizeEnabledProperty() { return resizeEnabled; }

    public int getTargetWidth() { return targetWidth.get(); }
    public void setTargetWidth(int v) { targetWidth.set(v); }
    public IntegerProperty targetWidthProperty() { return targetWidth; }

    public int getTargetHeight() { return targetHeight.get(); }
    public void setTargetHeight(int v) { targetHeight.set(v); }
    public IntegerProperty targetHeightProperty() { return targetHeight; }

    public FeatureMode getFeatureMode() { return featureMode.get(); }
    public void setFeatureMode(FeatureMode m) { featureMode.set(m); }
    public ObjectProperty<FeatureMode> featureModeProperty() { return featureMode; }

    public double getCannyLow() { return cannyLow.get(); }
    public void setCannyLow(double v) { cannyLow.set(v); }
    public DoubleProperty cannyLowProperty() { return cannyLow; }

    public double getCannyHigh() { return cannyHigh.get(); }
    public void setCannyHigh(double v) { cannyHigh.set(v); }
    public DoubleProperty cannyHighProperty() { return cannyHigh; }

    public double getMinContourArea() { return minContourArea.get(); }
    public void setMinContourArea(double v) { minContourArea.set(v); }
    public DoubleProperty minContourAreaProperty() { return minContourArea; }

    public double getConfidenceThreshold() { return confidenceThreshold.get(); }
    public void setConfidenceThreshold(double v) { confidenceThreshold.set(v); }
    public DoubleProperty confidenceThresholdProperty() { return confidenceThreshold; }

    public boolean isInferenceEnabled() { return inferenceEnabled.get(); }
    public void setInferenceEnabled(boolean v) { inferenceEnabled.set(v); }
    public BooleanProperty inferenceEnabledProperty() { return inferenceEnabled; }

    public ObservableSet<String> getClassFilter() { return classFilter; }
    public ObservableList<DecisionRule> getRules() { return rules; }
}
