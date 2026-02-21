package com.cricket.engine;

public enum BallOutcome {

    DOT(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    SIX(6),
    WICKET(-1);

    private final int runs;

    BallOutcome(int runs) {
        this.runs = runs;
    }

    public int getRuns() {
        return runs;
    }

    public boolean isWicket() {
        return this == WICKET;
    }
}