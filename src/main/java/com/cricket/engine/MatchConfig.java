package com.cricket.engine;

import java.util.List;

/**
 * Holds all configuration chosen in the GUI before the match runs.
 * Passed between screens and handed to MatchLauncher at the end.
 */
public class MatchConfig {

    // Teams
    public final String teamAName;
    public final List<String> teamAXI;
    public final String teamBName;
    public final List<String> teamBXI;

    // Bowling plans (over-by-over, built in GUI)
    public BowlingPlan teamABowlingPlan;  // Team A bowls at Team B
    public BowlingPlan teamBBowlingPlan;  // Team B bowls at Team A

    // Pitch
    public PitchProfile pitchProfile;

    public MatchConfig(String teamAName, List<String> teamAXI,
                       String teamBName, List<String> teamBXI) {
        this.teamAName = teamAName;
        this.teamAXI   = teamAXI;
        this.teamBName = teamBName;
        this.teamBXI   = teamBXI;
    }
}