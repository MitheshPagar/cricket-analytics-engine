package com.cricket;

public class Stats {

    private int balls;
    private int runs;
    private int dismissals;

    private static final int CONFIDENCE = 500;

    public void recordBall(int runsInBall, boolean isDismissal) {
        balls++;
        runs += runsInBall;

        if (isDismissal) {
            dismissals++;
        }
    }

    public int getBalls() {
        return balls;
    }

    public int getRuns() {
        return runs;
    }

    public int getDismissals() {
        return dismissals;
    }

    public double getRunsPerBall() {
        if (balls == 0) return 0.0;
        return (double) runs / balls;
    }

    public double getWicketsPerBall() {
        if (balls == 0) return 0.0;
        return (double) dismissals / balls;
    }

    public double getBattingAverage() {
        if (dismissals == 0) {
            
            return runs;
        }
        return (double) runs / dismissals;
    }

    public double getBattingStrikeRate() {
        if (balls == 0) return 0.0;
        return ((double) runs / balls) * 100.0;
    }

    public double getBowlingAverage() {
        if (dismissals == 0) {
            
            return runs;
        }
        return (double) runs / dismissals;
    }

    public double getBowlingStrikeRate() {
        if (dismissals == 0) return 0.0;
        return (double) balls / dismissals;
    }

    public double getEconomy() {
        if (balls == 0) return 0.0;
        return ((double) runs / balls) * 6.0;
    }

    public double getSampleWeight() {
        /*
         Weight formula:
         weight = balls / (balls + 500)

         Meaning:
         - 10 balls  -> ~0.02 (heavily nerfed)
         - 100 balls -> ~0.17
         - 500 balls -> 0.50 (balanced)
         - 2000 balls -> ~0.80 (true skill dominates)
        */
        return (double) balls / (balls + CONFIDENCE);
    }


    /**
     * Adjusted Runs Per Ball using role-specific baseline.
     * Prevents small sample batters from becoming overpowered.
     */
    public double getAdjustedRunsPerBall(double baselineRunsPerBall) {

        if (balls == 0) {
            return baselineRunsPerBall;
        }

        double playerRPB = getRunsPerBall();
        double weight = getSampleWeight();

        return (playerRPB * weight) +
               (baselineRunsPerBall * (1.0 - weight));
    }

    /**
     * Adjusted Wickets Per Ball using baseline.
     * Prevents fake elite bowlers from tiny samples.
     */
    public double getAdjustedWicketsPerBall(double baselineWicketsPerBall) {

        if (balls == 0) {
            return baselineWicketsPerBall;
        }

        double playerWPB = getWicketsPerBall();
        double weight = getSampleWeight();

        return (playerWPB * weight) +
               (baselineWicketsPerBall * (1.0 - weight));
    }

    public boolean isSmallSample() {
        return balls < 100;
    }

    public int getConfidenceConstant() {
        return CONFIDENCE;
    }
}
