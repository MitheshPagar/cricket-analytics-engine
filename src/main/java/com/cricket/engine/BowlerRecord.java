package com.cricket.engine;

public class BowlerRecord {
    public final String name;
    public int ballsBowled = 0;
    public int runsConceded = 0;
    public int wickets = 0;

    public BowlerRecord(String name) { this.name = name; }

    public void record(int runs, boolean isWicket) {
        ballsBowled++;
        runsConceded += runs;
        if (isWicket) wickets++;
    }

    public String overs() {
        return (ballsBowled / 6) + "." + (ballsBowled % 6);
    }

    public double economy() {
        return ballsBowled == 0 ? 0.0 : (runsConceded * 6.0) / ballsBowled;
    }
}