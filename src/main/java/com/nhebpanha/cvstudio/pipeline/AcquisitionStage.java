package com.nhebpanha.cvstudio.pipeline;

import com.nhebpanha.cvstudio.model.Frame;
import com.nhebpanha.cvstudio.model.PipelineSettings;
import org.opencv.core.Mat;

/**
 * Stage 1: Image Acquisition.
 * Receives and validates the acquired image from file or live camera source.
 */
public class AcquisitionStage implements PipelineStage {

    @Override
    public String name() {
        return "1 Acquisition";
    }

    @Override
    public Frame apply(Frame frame, PipelineSettings settings) {
        Mat original = frame.getOriginal();
        if (original == null || original.empty()) {
            throw new IllegalArgumentException("Acquired frame is empty or null.");
        }
        return frame;
    }
}
