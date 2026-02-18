package com.cricket;

public class Stats {
    private int balls = 0;
    private int runs = 0;
    private int dismissals = 0;

    public void recordBall(int runsInBall, boolean isDismissal){
        balls++;
        runs += runsInBall;

        if(isDismissal){
            dismissals++;
        }
    }

    public double getRunsPerBall(){
        return balls == 0 ? 0 : (double) runs / balls;
    }

    public double getWicketsPerBall(){
        return balls == 0 ? 0 : (double) dismissals / balls;
    }

    public double getBattingAverage(){
        return dismissals == 0 ? runs : (double) runs / dismissals;
    }

    public double getBattingStrikeRate(){
        return balls == 0 ? 0 : ((double) runs / balls) * 100;
    }

    public double getBowlingAverage(){
        return dismissals == 0 ? 0 : (double) runs / dismissals;
    }

    public double getBowlingStrikeRate(){
        return dismissals == 0 ? 0 : (double) balls / dismissals;
    }

    public double getEconomy(){
        return balls == 0 ? 0 : ((double) runs / balls) * 6;
    }

    public int getBalls(){
        return balls;
    }

    public int getRuns(){
        return runs;
    }

    public int getDismissals(){
        return dismissals;
    }
}
