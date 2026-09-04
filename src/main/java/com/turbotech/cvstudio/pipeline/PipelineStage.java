package com.turbotech.cvstudio.pipeline;

import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;

/**
 * Common contract for all vision pipeline stages.
 * Pipeline stages operate purely on image data and observable settings without UI dependencies.
 */
public interface PipelineStage {
    String name();

    Frame apply(Frame frame, PipelineSettings settings);

    default boolean enabled(PipelineSettings settings) {
        return true;
    }
}
