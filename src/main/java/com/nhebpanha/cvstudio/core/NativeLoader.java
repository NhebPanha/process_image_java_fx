package com.nhebpanha.cvstudio.core;

/**
 * Ensures OpenCV native libraries are extracted and loaded exactly once.
 */
public final class NativeLoader {
    private static volatile boolean loaded = false;

    private NativeLoader() {}

    public static synchronized void load() {
        if (!loaded) {
            nu.pattern.OpenCV.loadLocally();
            loaded = true;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
