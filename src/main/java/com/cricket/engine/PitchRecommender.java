package com.cricket.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Given pitch condition values, recommends which bowler categories
 * are favoured and at which over ranges.
 */
public class PitchRecommender {

    private double green;
    private double dry;
    private double bounce;
    private double flat;
    private double boundary;

    public PitchRecommender(double green, double dry, double bounce,
                            double flat, double boundary) {
        this.green = green;
        this.dry = dry;
        this.bounce = bounce;
        this.flat = flat;
        this.boundary = boundary;
    }

    public void update(double green, double dry, double bounce,
                       double flat, double boundary) {
        this.green = green;
        this.dry = dry;
        this.bounce = bounce;
        this.flat = flat;
        this.boundary = boundary;
    }

    /**
     * Returns recommended categories for a given over number.
     * Over deterioration is simulated: dry increases, green decreases with overs.
     */
    public List<BowlerInfo.Category> getRecommendedCategories(int overNumber) {
        List<BowlerInfo.Category> recommended = new ArrayList<>();

        // Simulate pitch deterioration by over
        double effectiveGreen = green * Math.pow(0.97, overNumber / 10.0);
        double effectiveDry   = dry   * Math.pow(1.04, overNumber / 10.0);

        boolean seamFavoured = effectiveGreen > 1.1 || bounce > 1.15;
        boolean spinFavoured = effectiveDry > 1.15;
        boolean newBall      = overNumber <= 10 || overNumber >= 80;

        if (newBall || seamFavoured) {
            recommended.add(BowlerInfo.Category.FAST);
            recommended.add(BowlerInfo.Category.MEDIUM_FAST);
        }

        if (!newBall) {
            recommended.add(BowlerInfo.Category.MEDIUM);
        }

        if (spinFavoured || (!newBall && overNumber > 20)) {
            recommended.add(BowlerInfo.Category.SPIN);
        }

        return recommended;
    }

    /**
     * Returns a human-readable pitch summary for the UI header.
     */
    public String getPitchSummary() {
        List<String> traits = new ArrayList<>();
        if (green > 1.2)   traits.add("Green");
        if (dry > 1.2)     traits.add("Dry");
        if (bounce > 1.2)  traits.add("Bouncy");
        if (flat > 1.3)    traits.add("Flat");
        if (green < 0.9 && dry < 0.9 && bounce < 0.9) traits.add("Dead");
        return traits.isEmpty() ? "Neutral" : String.join(" / ", traits);
    }

    public double getGreen()    { return green; }
    public double getDry()      { return dry; }
    public double getBounce()   { return bounce; }
    public double getFlat()     { return flat; }
    public double getBoundary() { return boundary; }
}