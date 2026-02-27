package com.cricket.engine;

public class InningsResult {

    private final int runs;
    private final int wickets;
    private final int balls;
    private final boolean declared;

    public InningsResult(int runs, int wickets, int balls) {
        this(runs, wickets, balls, false);
    }

    public InningsResult(int runs, int wickets, int balls, boolean declared) {
        this.runs = runs;
        this.wickets = wickets;
        this.balls = balls;
        this.declared = declared;
    }

    public int getRuns() { return runs; }
    public int getWickets() { return wickets; }
    public int getBalls() { return balls; }
    public boolean isDeclared() { return declared; }

    public String getOvers() {
        int fullOvers = balls / 6;
        int remainingBalls = balls % 6;
        return fullOvers + "." + remainingBalls;
    }

    public double getRunRate() {
        return balls > 0 ? (runs * 6.0) / balls : 0.0;
    }

    @Override
    public String toString() {
        String suffix = declared ? " dec" : "";
        return runs + "/" + wickets + suffix
                + " in " + getOvers()
                + " overs (RR: " + String.format("%.2f", getRunRate()) + ")";
    }
}