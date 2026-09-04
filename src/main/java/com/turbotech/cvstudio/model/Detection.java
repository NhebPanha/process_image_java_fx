package com.turbotech.cvstudio.model;

/**
 * An object detection result representing label, confidence score, and bounding box coordinates.
 */
public record Detection(
        String label,
        double confidence,
        int x,
        int y,
        int w,
        int h
) {
    public String getLabel() { return label; }
    public double getConfidence() { return confidence; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getW() { return w; }
    public int getH() { return h; }

    public String formattedConfidence() {
        return String.format("%.1f%%", confidence * 100);
    }

    public String formattedBox() {
        return String.format("[%d, %d, %d, %d]", x, y, w, h);
    }
}
