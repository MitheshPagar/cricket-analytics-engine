package com.cricket.engine;

import java.util.HashMap;
import java.util.Map;

import com.cricket.Main;
import com.cricket.StatsBundle;

public class MonteCarloEngine {

    public static class SimResult {
        public int teamAWins = 0;
        public int teamBWins = 0;
        public int draws     = 0;
        public int total     = 0;

        // ── Batting aggregates ─────────────────────────────────────────────
        public Map<String, Long>    batRuns        = new HashMap<>();
        public Map<String, Integer> batInnings     = new HashMap<>();
        public Map<String, Long>    batBalls       = new HashMap<>();
        public Map<String, Integer> batHundreds    = new HashMap<>();
        public Map<String, Integer> batFifties     = new HashMap<>();
        public Map<String, Integer> batHighest     = new HashMap<>(); // highest in a single innings

        // ── Bowling aggregates ─────────────────────────────────────────────
        public Map<String, Integer> bowlInnings    = new HashMap<>();
        public Map<String, Long>    bowlWickets    = new HashMap<>();
        public Map<String, Long>    bowlRuns       = new HashMap<>();
        public Map<String, Long>    bowlBalls      = new HashMap<>();
        public Map<String, Integer> bowlFifers     = new HashMap<>(); // 5wi in an innings
        public Map<String, Integer> bowlTenFor     = new HashMap<>(); // 10wm in a match
        // Best bowling: store as "W-R" string, pick best
        public Map<String, int[]>   bowlBest       = new HashMap<>(); // [wickets, runs]

        // ── Display helpers ───────────────────────────────────────────────
        public String topRunScorer() {
            return batRuns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> {
                        int inn = batInnings.getOrDefault(e.getKey(), 1);
                        double avg = e.getValue() / (double) inn;
                        return e.getKey() + "  —  " + e.getValue() + " runs  (avg "
                                + String.format("%.1f", avg) + ")";
                    }).orElse("N/A");
        }

        public String topWicketTaker() {
            return bowlWickets.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + "  —  " + e.getValue() + " wickets")
                    .orElse("N/A");
        }

        // Convenience for ReviewScreen (was totalRuns/totalWickets/inningsPlayed)
        public Map<String, Long>    getTotalRuns()     { return batRuns; }
        public Map<String, Long>    getTotalWickets()  { return bowlWickets; }
        public Map<String, Integer> getInningsPlayed() { return batInnings; }
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

        // Track match-level wickets per bowler for 10-wm detection
        for (int i = 0; i < simCount; i++) {
            try {
                BallEngine ballEngine = new BallEngine(
                        bundle.batterStats, bundle.bowlerStats,
                        bundle.baselineCalculator, config.pitchProfile);
                InningsEngine inningsEngine = new InningsEngine(ballEngine, bundle.roleLoader);
                SilentMatchEngine engine = new SilentMatchEngine(inningsEngine, config.pitchProfile);

                String outcome = engine.simulate(
                        config.teamAName, config.teamAXI,
                        config.teamBName, config.teamBXI,
                        config.teamABowlingPlan, config.teamBBowlingPlan);

                if      (outcome.contains(config.teamAName + " wins")) result.teamAWins++;
                else if (outcome.contains(config.teamBName + " wins")) result.teamBWins++;
                else result.draws++;

                // Per-match bowling wicket totals for 10wm
                Map<String, Integer> matchWickets = new HashMap<>();

                for (InningsResult ir : engine.getAllInnings()) {
                    // ── Batting ────────────────────────────────────────────
                    for (BatterRecord b : ir.getBattingCard()) {
                        if (b.balls == 0) continue;
                        result.batRuns.merge(b.name,  (long) b.runs, Long::sum);
                        result.batBalls.merge(b.name, (long) b.balls, Long::sum);
                        result.batInnings.merge(b.name, 1, Integer::sum);
                        if (b.runs >= 100) result.batHundreds.merge(b.name, 1, Integer::sum);
                        else if (b.runs >= 50) result.batFifties.merge(b.name, 1, Integer::sum);
                        result.batHighest.merge(b.name, b.runs, Integer::max);
                    }

                    // ── Bowling ────────────────────────────────────────────
                    for (BowlerRecord b : ir.getBowlingCard()) {
                        if (b.ballsBowled == 0) continue;
                        result.bowlInnings.merge(b.name, 1, Integer::sum);
                        result.bowlWickets.merge(b.name, (long) b.wickets, Long::sum);
                        result.bowlRuns.merge(b.name,   (long) b.runsConceded, Long::sum);
                        result.bowlBalls.merge(b.name,  (long) b.ballsBowled, Long::sum);

                        // 5-wicket haul
                        if (b.wickets >= 5) result.bowlFifers.merge(b.name, 1, Integer::sum);

                        // Best bowling figures
                        int[] curr = result.bowlBest.get(b.name);
                        if (curr == null || b.wickets > curr[0]
                                || (b.wickets == curr[0] && b.runsConceded < curr[1])) {
                            result.bowlBest.put(b.name, new int[]{b.wickets, b.runsConceded});
                        }

                        // Accumulate match wickets
                        matchWickets.merge(b.name, b.wickets, Integer::sum);
                    }
                }

                // 10-wicket match
                for (Map.Entry<String, Integer> e : matchWickets.entrySet()) {
                    if (e.getValue() >= 10) result.bowlTenFor.merge(e.getKey(), 1, Integer::sum);
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