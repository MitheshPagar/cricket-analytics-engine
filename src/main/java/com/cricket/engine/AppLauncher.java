package com.cricket.engine;

/**
 * Standalone launcher class that is NOT a JavaFX Application subclass.
 * This is the standard workaround for "JavaFX runtime components are missing"
 * when running a fat JAR — the JVM needs a non-JavaFX main class as entry point.
 */
public class AppLauncher {
    public static void main(String[] args) {
        BowlingAllocatorApp.main(args);
    }
}