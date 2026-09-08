package com.rupp.trafficvision;

/**
 * Main method wrapper that does not directly extend
 * {@link javafx.application.Application}.
 * Allows execution from standard classpaths, IDE run targets, and fat JARs
 * without triggering
 * "JavaFX runtime components are missing" module path errors.
 */
public class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
