package com.cricket.engine;

import java.util.ArrayList;
import java.util.List;

public class InningsResult {

    private final int runs;
    private final int wickets;
    private final int balls;
    private final boolean declared;

    // Detailed scorecard data
    private final List<BatterRecord>    battingCard;
    private final List<BowlerRecord>    bowlingCard;
    private final List<FallOfWicket>    fallOfWickets;
    private final List<Partnership>     partnerships;

    // Simple constructor (backward compat — no detail)
    public InningsResult(int runs, int wickets, int balls) {
        this(runs, wickets, balls, false,
             new ArrayList<>(), new ArrayList<>(),
             new ArrayList<>(), new ArrayList<>());
    }

    public InningsResult(int runs, int wickets, int balls, boolean declared) {
        this(runs, wickets, balls, declared,
             new ArrayList<>(), new ArrayList<>(),
             new ArrayList<>(), new ArrayList<>());
    }

    public InningsResult(int runs, int wickets, int balls, boolean declared,
                         List<BatterRecord> battingCard,
                         List<BowlerRecord> bowlingCard,
                         List<FallOfWicket> fallOfWickets,
                         List<Partnership>  partnerships) {
        this.runs          = runs;
        this.wickets       = wickets;
        this.balls         = balls;
        this.declared      = declared;
        this.battingCard   = battingCard;
        this.bowlingCard   = bowlingCard;
        this.fallOfWickets = fallOfWickets;
        this.partnerships  = partnerships;
    }

    public int getRuns()       { return runs; }
    public int getWickets()    { return wickets; }
    public int getBalls()      { return balls; }
    public boolean isDeclared(){ return declared; }

    public List<BatterRecord>  getBattingCard()   { return battingCard; }
    public List<BowlerRecord>  getBowlingCard()   { return bowlingCard; }
    public List<FallOfWicket>  getFallOfWickets() { return fallOfWickets; }
    public List<Partnership>   getPartnerships()  { return partnerships; }

    public String getOvers() {
        return (balls / 6) + "." + (balls % 6);
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