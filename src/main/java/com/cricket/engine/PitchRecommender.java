package com.cricket.engine;

import java.util.ArrayList;
import java.util.List;

public class PitchRecommender {

    private double green;
    private double dry;
    private double bounce;
    private double flat;
    private double boundary;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public PitchRecommender(double green, double dry, double bounce,
                            double flat, double boundary) {
        update(green, dry, bounce, flat, boundary);
    }

    public void update(double green, double dry, double bounce,
                       double flat, double boundary) {
        this.green    = green;
        this.dry      = dry;
        this.bounce   = bounce;
        this.flat     = flat;
        this.boundary = boundary;
    }

    /**
     * Returns recommended categories for a given over number.
     *
     * Logic:
     *  - Compute a seam score and spin score from pitch values.
     *  - Seam score driven by green + bounce; spin score driven by dry.
     *  - Flat pitch suppresses both — neither type is especially effective.
     *  - New ball overs (1-15) always prefer FAST regardless.
     *  - Spin is only recommended when spin score EXCEEDS seam score,
     *    or when it's clearly a turning pitch (dry > 1.3).
     *  - On a 2.0 green pitch, seam score is so dominant spin never appears.
     */
    public List<BowlerInfo.Category> getRecommendedCategories(int overNumber) {
        List<BowlerInfo.Category> recommended = new ArrayList<>();

        // Simulate pitch deterioration over time
        double effectiveGreen = green * Math.pow(0.96, overNumber / 10.0);
        double effectiveDry   = dry   * Math.pow(1.05, overNumber / 10.0);
        double effectiveBounce = bounce;

        // ── Score computation ──────────────────────────────────────────────
        // Seam score: green + bounce both help seam bowlers
        double seamScore = ((effectiveGreen - 1.0) * 2.0)   // green heavily weighted
                         + ((effectiveBounce - 1.0) * 1.0)  // bounce secondary
                         - ((flat - 1.0) * 1.5);            // flat suppresses seam

        // Spin score: dry helps spin, flat slightly helps spin, green kills spin
        double spinScore = ((effectiveDry - 1.0) * 2.0)     // dry heavily weighted
                         + ((flat - 1.0) * 0.5)             // flat mildly helps spin
                         - ((effectiveGreen - 1.0) * 2.0);  // green strongly hurts spin

        boolean newBall    = overNumber <= 15 || overNumber >= 80;
        boolean seamFavour = seamScore > 0.05;
        boolean spinFavour = spinScore > 0.05 && spinScore > seamScore;
        boolean clearlyTurning = effectiveDry > 1.3 && effectiveGreen < 1.1;

        // ── Category selection ────────────────────────────────────────────
        if (newBall || seamFavour) {
            recommended.add(BowlerInfo.Category.FAST);
            recommended.add(BowlerInfo.Category.MEDIUM_FAST);
        }

        // Medium pacers fill in when conditions are neither seam nor spin dominated
        if (!newBall && !clearlyTurning) {
            recommended.add(BowlerInfo.Category.MEDIUM);
        }

        // Spin only when pitch genuinely favours it AND seam doesn't dominate
        if (clearlyTurning || spinFavour) {
            recommended.add(BowlerInfo.Category.SPIN);
        }

        // Fallback: if nothing recommended (neutral pitch, mid-overs), add medium + spin
        if (recommended.isEmpty()) {
            recommended.add(BowlerInfo.Category.MEDIUM);
            if (overNumber > 25) recommended.add(BowlerInfo.Category.SPIN);
        }

        return recommended;
    }

    public String getPitchSummary() {
        List<String> traits = new ArrayList<>();
        if (green > 1.2)  traits.add("Green");
        if (dry > 1.2)    traits.add("Dry");
        if (bounce > 1.2) traits.add("Bouncy");
        if (flat > 1.3)   traits.add("Flat");
        if (green < 0.9 && dry < 0.9 && bounce < 0.9) traits.add("Dead");
        return traits.isEmpty() ? "Neutral" : String.join(" / ", traits);
    }

    public double getGreen()    { return green; }
    public double getDry()      { return dry; }
    public double getBounce()   { return bounce; }
    public double getFlat()     { return flat; }
    public double getBoundary() { return boundary; }
}