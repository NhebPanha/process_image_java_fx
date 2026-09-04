package com.nhebpanha.cvstudio;

/**
 * Bootstrap launcher class to bypass JavaFX runtime checks on the classpath.
 * Fixes: "Error: JavaFX runtime components are missing, and are required to run this application"
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}