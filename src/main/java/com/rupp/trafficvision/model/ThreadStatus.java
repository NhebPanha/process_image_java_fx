package com.rupp.trafficvision.model;

/**
 * Enumeration of thread execution states monitored by the Thread Monitor.
 */
public enum ThreadStatus {
    IDLE("Idle", "#6B7280"),
    PROCESSING("Processing", "#3B82F6"),
    WAITING("Waiting", "#F59E0B"),
    COMPLETED("Completed", "#10B981"),
    ERROR("Error", "#EF4444"),
    STOPPED("Stopped", "#9CA3AF");

    private final String label;
    private final String colorHex;

    ThreadStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return label;
    }
}
