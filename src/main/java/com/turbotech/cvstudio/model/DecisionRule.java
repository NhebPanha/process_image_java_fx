package com.turbotech.cvstudio.model;

/**
 * A configurable business rule evaluated during Stage 5 (Decision Making).
 */
public record DecisionRule(
        String name,
        String targetClass,
        int minCount,
        double minConfidence,
        Action action
) {
    public String getName() { return name; }
    public String getTargetClass() { return targetClass; }
    public int getMinCount() { return minCount; }
    public double getMinConfidence() { return minConfidence; }
    public Action getAction() { return action; }

    public enum Action {
        LOG,
        HIGHLIGHT,
        ALERT,
        SNAPSHOT,
        WEBHOOK
    }
}
