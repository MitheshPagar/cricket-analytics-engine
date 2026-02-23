package com.cricket.engine;

public class InningsResult {
    @SuppressWarnings("FieldMayBeFinal")
    private int runs;
    @SuppressWarnings("FieldMayBeFinal")
    private int wickets;
    @SuppressWarnings("FieldMayBeFinal")
    private int balls;

    public InningsResult(int runs, int wickets, int balls) {
        this.runs = runs;
        this.wickets = wickets;
        this.balls = balls;
    }

    public int getRuns() {
        return runs;
    }

    public int getWickets() {
        return wickets;
    }

    public int getBalls() {
        return balls;
    }

    public String getOvers(){
        int fullOvers = balls / 6;
        int remainingBalls = balls % 6;
        return fullOvers + "." + remainingBalls;
    }

    public double getRunRate() {
        return balls > 0 ? (runs * 6.0) / balls : 0.0;
    }

    @Override
    public String toString() {
        return runs + "/" + wickets + " in " + getOvers() + " overs (RR: " + String.format("%.2f", getRunRate()) + ")";
    }
}
