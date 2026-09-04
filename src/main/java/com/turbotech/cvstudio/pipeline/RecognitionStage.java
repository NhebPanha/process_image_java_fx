package com.turbotech.cvstudio.pipeline;

import com.turbotech.cvstudio.core.ModelService;
import com.turbotech.cvstudio.model.Detection;
import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;
import com.turbotech.cvstudio.util.EventLog;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4: Object Recognition and Classification.
 * Applies AI / DNN inference to detect objects, filter by confidence thresholds, and active class filters.
 */
public class RecognitionStage implements PipelineStage {

    private final ModelService modelService;

    public RecognitionStage(ModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public String name() {
        return "4 Recognition & Classification";
    }

    @Override
    public boolean enabled(PipelineSettings settings) {
        return settings.isInferenceEnabled();
    }

    @Override
    public Frame apply(Frame frame, PipelineSettings s) {
        Mat mat = frame.getProcessed() != null ? frame.getProcessed() : frame.getOriginal();
        if (mat == null || mat.empty()) {
            return frame;
        }

        try {
            List<Detection> found = modelService.detect(mat, s.getConfidenceThreshold());
            List<Detection> filtered = new ArrayList<>();

            for (Detection d : found) {
                if (d.confidence() >= s.getConfidenceThreshold()) {
                    if (s.getClassFilter().isEmpty() || s.getClassFilter().contains(d.label().toLowerCase())) {
                        filtered.add(d);
                    }
                }
            }

            frame.setDetections(filtered);
        } catch (Exception ex) {
            EventLog.error("Inference failed: " + ex.getMessage());
        }

        return frame;
    }
}
