package com.cricket.engine;

public class PitchProfile {

    private final double greenFactor;
    private final double dryFactor;
    private final double bounceFactor;
    private final double flatFactor;
    private final double boundaryFactor;

    public PitchProfile(double greenFactor,
                        double dryFactor,
                        double bounceFactor,
                        double flatFactor,
                        double boundaryFactor) {

        this.greenFactor = clamp(greenFactor);
        this.dryFactor = clamp(dryFactor);
        this.bounceFactor = clamp(bounceFactor);
        this.flatFactor = clamp(flatFactor);
        this.boundaryFactor = clamp(boundaryFactor);
    }

    private double clamp(double value) {
        return Math.max(0.5, Math.min(2.0, value));
    }

    public double getGreenFactor() { return greenFactor; }
    public double getDryFactor() { return dryFactor; }
    public double getBounceFactor() { return bounceFactor; }
    public double getFlatFactor() { return flatFactor; }
    public double getBoundaryFactor() { return boundaryFactor; }

    public static PitchProfile neutral() {
        return new PitchProfile(1.0, 1.0, 1.0, 1.0, 1.0);
    }
}