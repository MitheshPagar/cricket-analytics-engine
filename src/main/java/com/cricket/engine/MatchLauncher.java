package com.cricket.engine;

import com.cricket.Main;
import com.cricket.StatsBundle;

/**
 * Bridges the GUI bowling plans + pitch config into the simulation engine.
 * Called once the user clicks "Run Match" on the review screen.
 */
public class MatchLauncher {

    /**
     * Builds the stats pipeline, wires up the engines with the
     * GUI-configured pitch and bowling plans, and runs the match.
     */
    public static void launch(MatchConfig config) {

        System.out.println("\n========================================");
        System.out.println("  LAUNCHING MATCH: "
                + config.teamAName + " vs " + config.teamBName);
        System.out.println("========================================\n");

        try {
            // Build stats from match data
            System.out.println("Building stats pipeline...");
            StatsBundle bundle = Main.buildStats();

            // Wire engines
            BallEngine ballEngine = new BallEngine(
                    bundle.batterStats,
                    bundle.bowlerStats,
                    bundle.baselineCalculator,
                    config.pitchProfile
            );

            InningsEngine inningsEngine =
                    new InningsEngine(ballEngine, bundle.roleLoader);

            TestMatchEngine matchEngine =
                    new TestMatchEngine(inningsEngine, config.pitchProfile);

            // Run match — bowling plans are passed so InningsEngine
            // uses the GUI-allocated over sequence
            matchEngine.simulateMatch(
                    config.teamAName, config.teamAXI,
                    config.teamBName, config.teamBXI,
                    config.teamABowlingPlan,
                    config.teamBBowlingPlan
            );

        } catch (Exception e) {
            System.err.println("Match failed to launch: " + e.getMessage());
            e.printStackTrace();
        }
    }
}