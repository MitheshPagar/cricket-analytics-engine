package com.cricket.engine;

public class DeterioratingPitch {

    private double green;
    private double dry;
    private double bounce;
    private double flat;
    private double boundary;

    public DeterioratingPitch(PitchProfile base) {
        this.green = base.getGreen();
        this.dry = base.getDry();
        this.bounce = base.getBounce();
        this.flat = base.getFlat();
        this.boundary = base.getBoundary();
    }

    public PitchProfile currentProfile() {
        return new PitchProfile(green, dry, bounce, flat, boundary);
    }

    public void deteriorate() {

        // Grass fades
        green *= 0.92;

        // Bounce reduces slightly
        bounce *= 0.97;

        // Dryness increases (rough develops)
        dry *= 1.08;

        // Pitch becomes harder to bat
        flat *= 0.97;

        // Outfield slows slightly
        boundary *= 0.98;

        clamp();
    }

    private void clamp() {
        green = clampValue(green);
        dry = clampValue(dry);
        bounce = clampValue(bounce);
        flat = clampValue(flat);
        boundary = clampValue(boundary);
    }

    private double clampValue(double value) {
        return Math.max(0.5, Math.min(2.0, value));
    }
}