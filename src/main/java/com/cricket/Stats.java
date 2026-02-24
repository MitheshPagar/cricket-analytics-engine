package com.cricket;

public class Stats {

    private int balls;
    private int runs;
    private int dismissals;

    private static final int CONFIDENCE = 1000;

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
        return (double) balls / (balls + CONFIDENCE);
    }


    public double getAdjustedRunsPerBall(double baselineRunsPerBall) {

        if (balls == 0) {
            return baselineRunsPerBall;
        }

        double playerRPB = getRunsPerBall();
        double weight = getSampleWeight();

        return (playerRPB * weight) +
               (baselineRunsPerBall * (1.0 - weight));
    }

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
