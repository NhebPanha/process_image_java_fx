package com.turbotech.cvstudio.model;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

/**
 * Encapsulates extracted features from Stage 3 (Feature Extraction).
 */
public class FeatureSet {
    private Mat edges;
    private int contourCount;
    private MatOfKeyPoint keypoints;
    private Mat descriptors;
    private float[] histogram;

    public FeatureSet() {}

    public Mat getEdges() {
        return edges;
    }

    public void setEdges(Mat edges) {
        this.edges = edges;
    }

    public int getContourCount() {
        return contourCount;
    }

    public void setContourCount(int contourCount) {
        this.contourCount = contourCount;
    }

    public MatOfKeyPoint getKeypoints() {
        return keypoints;
    }

    public void setKeypoints(MatOfKeyPoint keypoints) {
        this.keypoints = keypoints;
    }

    public Mat getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(Mat descriptors) {
        this.descriptors = descriptors;
    }

    public float[] getHistogram() {
        return histogram;
    }

    public void setHistogram(float[] histogram) {
        this.histogram = histogram;
    }

    public void release() {
        if (edges != null) {
            edges.release();
            edges = null;
        }
        if (keypoints != null) {
            keypoints.release();
            keypoints = null;
        }
        if (descriptors != null) {
            descriptors.release();
            descriptors = null;
        }
    }
}
