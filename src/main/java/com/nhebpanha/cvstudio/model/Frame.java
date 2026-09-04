package com.nhebpanha.cvstudio.model;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the image data, intermediate transforms, and vision metadata across all pipeline stages.
 */
public class Frame {
    private Mat original;
    private Mat processed;
    private Mat featureView;
    private FeatureSet features;
    private final List<Detection> detections = new ArrayList<>();
    private String decision = "—";
    private long timestampMs = System.currentTimeMillis();
    private final List<StageTiming> timings = new ArrayList<>();
    private boolean highlight = false;

    public record StageTiming(String stage, long millis) {}

    public Frame(Mat original) {
        this.original = original;
    }

    public Mat getOriginal() {
        return original;
    }

    public void setOriginal(Mat original) {
        this.original = original;
    }

    public Mat getProcessed() {
        return processed;
    }

    public void setProcessed(Mat processed) {
        this.processed = processed;
    }

    public Mat getFeatureView() {
        return featureView;
    }

    public void setFeatureView(Mat featureView) {
        this.featureView = featureView;
    }

    public FeatureSet getFeatures() {
        return features;
    }

    public void setFeatures(FeatureSet features) {
        this.features = features;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    public void setDetections(List<Detection> list) {
        this.detections.clear();
        if (list != null) {
            this.detections.addAll(list);
        }
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestampMs) {
        this.timestampMs = timestampMs;
    }

    public List<StageTiming> getTimings() {
        return Collections.unmodifiableList(timings);
    }

    public void addTiming(String stage, long millis) {
        timings.add(new StageTiming(stage, millis));
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public long getTotalLatencyMs() {
        long sum = 0;
        for (StageTiming t : timings) {
            sum += t.millis();
        }
        return sum;
    }

    /**
     * Explicitly releases native memory associated with OpenCV Mats.
     */
    public void release() {
        if (original != null) {
            original.release();
            original = null;
        }
        if (processed != null) {
            processed.release();
            processed = null;
        }
        if (featureView != null) {
            featureView.release();
            featureView = null;
        }
        if (features != null) {
            features.release();
            features = null;
        }
    }
}
