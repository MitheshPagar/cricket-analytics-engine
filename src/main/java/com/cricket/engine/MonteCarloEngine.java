package com.cricket.engine;

import com.cricket.Main;
import com.cricket.StatsBundle;

import java.util.*;

/**
 * Runs N simulations of the match and aggregates results.
 */
public class MonteCarloEngine {

    public static class SimResult {
        public int teamAWins  = 0;
        public int teamBWins  = 0;
        public int draws      = 0;
        public int total      = 0;

        // Per-player run/wicket totals across all sims
        public Map<String, Long>   totalRuns    = new HashMap<>();
        public Map<String, Long>   totalWickets = new HashMap<>();
        public Map<String, Integer> inningsPlayed = new HashMap<>(); // for avg

        public double teamAWinPct()  { return total == 0 ? 0 : (teamAWins  * 100.0) / total; }
        public double teamBWinPct()  { return total == 0 ? 0 : (teamBWins  * 100.0) / total; }
        public double drawPct()      { return total == 0 ? 0 : (draws      * 100.0) / total; }

        public String topRunScorer() {
            return totalRuns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> {
                        int innings = inningsPlayed.getOrDefault(e.getKey(), 1);
                        double avg = e.getValue() / (double) innings;
                        return e.getKey() + "  —  " + e.getValue() + " runs  (avg "
                                + String.format("%.1f", avg) + ")";
                    }).orElse("N/A");
        }

        public String topWicketTaker() {
            return totalWickets.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + "  —  " + e.getValue() + " wickets")
                    .orElse("N/A");
        }
    }

    public static SimResult run(MatchConfig config, int simCount,
                                 java.util.function.Consumer<Integer> progressCallback) {
        SimResult result = new SimResult();

        StatsBundle bundle;
        try {
            bundle = Main.buildStats();
        } catch (Exception e) {
            System.err.println("Monte Carlo: failed to load stats: " + e.getMessage());
            return result;
        }

        for (int i = 0; i < simCount; i++) {
            try {
                BallEngine ballEngine = new BallEngine(
                        bundle.batterStats,
                        bundle.bowlerStats,
                        bundle.baselineCalculator,
                        config.pitchProfile
                );
                InningsEngine inningsEngine = new InningsEngine(ballEngine, bundle.roleLoader);
                SilentMatchEngine engine = new SilentMatchEngine(inningsEngine, config.pitchProfile);

                String outcome = engine.simulate(
                        config.teamAName, config.teamAXI,
                        config.teamBName, config.teamBXI,
                        config.teamABowlingPlan,
                        config.teamBBowlingPlan
                );

                if (outcome.contains(config.teamAName + " wins")) result.teamAWins++;
                else if (outcome.contains(config.teamBName + " wins")) result.teamBWins++;
                else result.draws++;

                // Aggregate player stats
                for (InningsResult ir : engine.getAllInnings()) {
                    for (BatterRecord b : ir.getBattingCard()) {
                        result.totalRuns.merge(b.name, (long) b.runs, Long::sum);
                        result.inningsPlayed.merge(b.name, 1, Integer::sum);
                    }
                    for (BowlerRecord b : ir.getBowlingCard()) {
                        result.totalWickets.merge(b.name, (long) b.wickets, Long::sum);
                    }
                }

                result.total++;
                if (progressCallback != null && i % 50 == 0) progressCallback.accept(i);

            } catch (Exception e) {
                // Skip failed simulations silently
            }
        }

        return result;
    }
}