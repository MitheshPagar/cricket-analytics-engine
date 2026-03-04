package com.cricket.engine;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Resolves file paths relative to the running JAR/EXE location.
 * Falls back to the working directory if the JAR path can't be determined.
 */
public class PathResolver {

    private static File baseDir = null;

    public static File getBaseDir() {
        if (baseDir != null) return baseDir;

        try {
            // Get the location of the running JAR
            File jar = new File(PathResolver.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            File dir = jar.isFile() ? jar.getParentFile() : jar;

            // Walk up from target/ to project root if playerRoles.csv not found here
            File candidate = new File(dir, "playerRoles.csv");
            if (!candidate.exists()) {
                File parent = dir.getParentFile();
                if (parent != null && new File(parent, "playerRoles.csv").exists()) {
                    dir = parent;
                }
            }

            baseDir = dir;
        } catch (URISyntaxException e) {
            baseDir = new File("."); // fallback to working directory
        }

        System.out.println("Base directory resolved to: " + baseDir.getAbsolutePath());
        return baseDir;
    }

    public static String resolve(String relativePath) {
        return new File(getBaseDir(), relativePath).getAbsolutePath();
    }

    public static Path resolvePath(String relativePath) {
        return new File(getBaseDir(), relativePath).toPath();
    }
}