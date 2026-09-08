package com.rupp.trafficvision.model;

/**
 * Enumeration of traffic light optical states.
 */
public enum TrafficLightColor {
    RED("Red", "#EF4444", "#991B1B"),
    YELLOW("Yellow", "#F59E0B", "#92400E"),
    GREEN("Green", "#10B981", "#065F46");

    private final String label;
    private final String activeColor;
    private final String dimColor;

    TrafficLightColor(String label, String activeColor, String dimColor) {
        this.label = label;
        this.activeColor = activeColor;
        this.dimColor = dimColor;
    }

    public String getLabel() {
        return label;
    }

    public String getActiveColor() {
        return activeColor;
    }

    public String getDimColor() {
        return dimColor;
    }

    /**
     * Cycles to the next color state in standard traffic light sequencing:
     * RED -> GREEN -> YELLOW -> RED.
     */
    public TrafficLightColor next() {
        return switch (this) {
            case RED -> GREEN;
            case GREEN -> YELLOW;
            case YELLOW -> RED;
        };
    }
}
