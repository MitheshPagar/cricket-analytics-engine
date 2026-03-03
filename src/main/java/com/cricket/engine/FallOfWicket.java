package com.cricket.engine;

public class FallOfWicket {
    public final int wicketNumber;
    public final String batterName;
    public final int score;
    public final int balls;

    public FallOfWicket(int wicketNumber, String batterName, int score, int balls) {
        this.wicketNumber = wicketNumber;
        this.batterName   = batterName;
        this.score        = score;
        this.balls        = balls;
    }

    public String overs() {
        return (balls / 6) + "." + (balls % 6);
    }
}