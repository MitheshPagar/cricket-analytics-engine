package com.cricket.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Like TestMatchEngine but produces no console output and no scorecard UI.
 * Used for Monte Carlo simulations.
 */
public class SilentMatchEngine {

    private final InningsEngine inningsEngine;
    private final DeterioratingPitch pitch;
    private final List<InningsResult> allInnings = new ArrayList<>();

    private int matchBalls = 0;
    private static final int MAX_MATCH_BALLS = 450 * 6;

    public SilentMatchEngine(InningsEngine inningsEngine, PitchProfile basePitch) {
        this.inningsEngine = inningsEngine;
        this.pitch = new DeterioratingPitch(basePitch);
    }

    public List<InningsResult> getAllInnings() { return allInnings; }

    public String simulate(String teamAName, List<String> teamA,
                           String teamBName, List<String> teamB,
                           BowlingPlan teamABowlingPlan,
                           BowlingPlan teamBBowlingPlan) {
        allInnings.clear();
        matchBalls = 0;

        DeclarationEngine decEngine = new DeclarationEngine();

        // 1st innings
        InningsResult aFirst = play(teamA, teamB, null, 1, 0, teamBBowlingPlan, decEngine);
        allInnings.add(aFirst);
        int a1 = aFirst.getRuns();
        if (timeUp()) return "Match Drawn";

        // 2nd innings
        InningsResult bFirst = play(teamB, teamA, null, 2, -a1, teamABowlingPlan, decEngine);
        allInnings.add(bFirst);
        int b1 = bFirst.getRuns();
        if (timeUp()) return "Match Drawn";

        int lead = a1 - b1;

        if (lead >= 200) {
            // Follow-on
            InningsResult bSecond = play(teamB, teamA, null, 3, b1 - a1, teamABowlingPlan, decEngine);
            allInnings.add(bSecond);
            int b2 = bSecond.getRuns();
            if (timeUp()) return "Match Drawn";

            if (b1 + b2 > a1) {
                int target = (b1 + b2) - a1 + 1;
                InningsResult aSecond = play(teamA, teamB, target, 4, 0, teamBBowlingPlan, decEngine);
                allInnings.add(aSecond);
                return chaseResult(aSecond, target, teamAName, teamBName);
            }
            int target = (a1 - b1 - b2) + 1;
            if (target <= 0) return teamAName + " wins by an innings";
            InningsResult aSecond = play(teamA, teamB, target, 4, 0, teamBBowlingPlan, decEngine);
            allInnings.add(aSecond);
            return chaseResult(aSecond, target, teamAName, teamBName);
        } else {
            // Normal
            InningsResult aSecond = play(teamA, teamB, null, 3, a1 - b1, teamBBowlingPlan, decEngine);
            allInnings.add(aSecond);
            int a2 = aSecond.getRuns();
            if (timeUp()) return "Match Drawn";

            int target = a1 + a2 - b1 + 1;
            InningsResult bSecond = play(teamB, teamA, target, 4, 0, teamABowlingPlan, decEngine);
            allInnings.add(bSecond);
            return chaseResult(bSecond, target, teamBName, teamAName);
        }
    }

    private InningsResult play(List<String> batting, List<String> bowling,
                                Integer target, int inningsNum, int lead,
                                BowlingPlan plan, DeclarationEngine decEngine) {
        inningsEngine.setPitch(pitch.currentProfile());
        int remaining = MAX_MATCH_BALLS - matchBalls;
        List<String> bowlingOrder = (plan != null)
                ? plan.toOrderedBowlingList(remaining / 6) : bowling;
        if (bowlingOrder == null || bowlingOrder.isEmpty()) bowlingOrder = bowling;

        DeclarationEngine dec = inningsNum < 4 ? decEngine : null;
        InningsResult r = inningsEngine.simulateInnings(
                batting, bowlingOrder, remaining, target, dec, inningsNum, lead, plan);
        matchBalls += r.getBalls();
        pitch.deteriorate();
        return r;
    }

    private boolean timeUp() { return matchBalls >= MAX_MATCH_BALLS; }

    private String chaseResult(InningsResult r, int target,
                                String chasing, String defending) {
        if (r.getRuns() >= target) return chasing + " wins by " + (10 - r.getWickets()) + " wickets";
        if (timeUp()) return "Match Drawn";
        return defending + " wins by " + (target - r.getRuns() - 1) + " runs";
    }
}