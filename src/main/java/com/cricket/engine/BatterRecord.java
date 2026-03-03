package com.cricket.engine;

public class BatterRecord {
    public final String name;
    public int runs    = 0;
    public int balls   = 0;
    public int fours   = 0;
    public int sixes   = 0;
    public boolean notOut = true;
    public String dismissalInfo = "not out";

    public BatterRecord(String name) { this.name = name; }

    public void record(int runs, boolean isWicket) {
        this.balls++;
        this.runs += runs;
        if (runs == 4) fours++;
        if (runs == 6) sixes++;
        if (isWicket) { notOut = false; }
    }

    public double strikeRate() {
        return balls == 0 ? 0.0 : (runs * 100.0) / balls;
    }
}