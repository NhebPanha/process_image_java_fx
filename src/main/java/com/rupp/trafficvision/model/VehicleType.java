package com.rupp.trafficvision.model;

/**
 * Enumeration of supported vehicle types monitored at the traffic light.
 * Supports classification into Cars, Motorcycles, Bicycles, Tricycles, and Mopeds.
 */
public enum VehicleType {
    CAR("Car", "🚗", "#3B82F6", 1.6),
    MOTORCYCLE("Motorcycle", "🏍", "#10B981", 0.85),
    BICYCLE("Bicycle", "🚲", "#F59E0B", 0.75),
    TRICYCLE("Tricycle", "🛺", "#EC4899", 1.15),
    MOPED("Moped", "🛵", "#8B5CF6", 0.80),
    UNKNOWN("Unknown", "❓", "#6B7280", 1.0);

    private final String displayName;
    private final String emoji;
    private final String colorHex;
    private final double typicalAspectRatio;

    VehicleType(String displayName, String emoji, String colorHex, double typicalAspectRatio) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.colorHex = colorHex;
        this.typicalAspectRatio = typicalAspectRatio;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getColorHex() {
        return colorHex;
    }

    public double getTypicalAspectRatio() {
        return typicalAspectRatio;
    }

    @Override
    public String toString() {
        return emoji + " " + displayName;
    }
}
