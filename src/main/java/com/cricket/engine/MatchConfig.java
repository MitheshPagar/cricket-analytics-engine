package com.cricket.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all configuration chosen in the GUI before the match runs.
 * Passed between screens and handed to MatchLauncher at the end.
 */
public class MatchConfig {

    // Teams — populated by TeamBuilderScreen / TeamSetupScreen
    public String teamAName = "";
    public List<String> teamAXI = new ArrayList<>();
    public String teamBName = "";
    public List<String> teamBXI = new ArrayList<>();

    // Bowling plans (over-by-over, built in GUI)
    public BowlingPlan teamABowlingPlan = null;  // Team A bowls at Team B
    public BowlingPlan teamBBowlingPlan = null;  // Team B bowls at Team A

    // Pitch
    public PitchProfile pitchProfile = null;
}