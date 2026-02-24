package com.cricket.engine;

public class PitchProfile {

    private final double green;
    private final double dry;
    private final double bounce;
    private final double flat;
    private final double boundary;

    public PitchProfile(double green,
                        double dry,
                        double bounce,
                        double flat,
                        double boundary) {

        this.green = clamp(green);
        this.dry = clamp(dry);
        this.bounce = clamp(bounce);
        this.flat= clamp(flat);
        this.boundary= clamp(boundary);
    }

    private double clamp(double value) {
        return Math.max(0.5, Math.min(2.0, value));
    }

    public double getGreen() { return green; }
    public double getDry() { return dry; }
    public double getBounce() { return bounce; }
    public double getFlat() { return flat; }
    public double getBoundary() { return boundary; }

    public static PitchProfile neutral() {
        return new PitchProfile(1.0, 1.0, 1.0, 1.0, 1.0);
    }
}