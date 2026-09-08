package com.rupp.trafficvision.model;

/**
 * Encapsulates tunable image preprocessing parameters and active filter toggles.
 */
public class ProcessingParameters {

    public enum Mode {
        ORIGINAL("Original"),
        GRAYSCALE("Grayscale"),
        EDGE_DETECTION("Edge Detection"),
        THRESHOLD("Threshold"),
        BLUR("Blur"),
        SHARPEN("Sharpen"),
        CONTRAST("Contrast"),
        VEHICLE_DETECTION("Vehicle Detection");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private Mode mode = Mode.VEHICLE_DETECTION;
    private double brightness = 0.0;    // -100 to 100
    private double contrast = 1.0;      // 0.5 to 2.5
    private int threshold = 128;        // 0 to 255
    private boolean resize = true;
    private boolean grayscale = false;
    private boolean noiseReduction = true;
    private boolean edgeDetection = false;
    private boolean contrastEnhancement = false;

    public ProcessingParameters() {}

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.ORIGINAL;
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = Math.max(-100.0, Math.min(100.0, brightness));
    }

    public double getContrast() {
        return contrast;
    }

    public void setContrast(double contrast) {
        this.contrast = Math.max(0.1, Math.min(3.0, contrast));
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = Math.max(0, Math.min(255, threshold));
    }

    public boolean isResize() {
        return resize;
    }

    public void setResize(boolean resize) {
        this.resize = resize;
    }

    public boolean isGrayscale() {
        return grayscale;
    }

    public void setGrayscale(boolean grayscale) {
        this.grayscale = grayscale;
    }

    public boolean isNoiseReduction() {
        return noiseReduction;
    }

    public void setNoiseReduction(boolean noiseReduction) {
        this.noiseReduction = noiseReduction;
    }

    public boolean isEdgeDetection() {
        return edgeDetection;
    }

    public void setEdgeDetection(boolean edgeDetection) {
        this.edgeDetection = edgeDetection;
    }

    public boolean isContrastEnhancement() {
        return contrastEnhancement;
    }

    public void setContrastEnhancement(boolean contrastEnhancement) {
        this.contrastEnhancement = contrastEnhancement;
    }
}
