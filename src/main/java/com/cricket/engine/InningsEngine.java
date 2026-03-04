package com.cricket.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.cricket.PlayerRoleLoader;

public class InningsEngine {

    private final BallEngine ballEngine;
    private final PlayerRoleLoader roleLoader;
    private final Random random = new Random();

    public InningsEngine(BallEngine ballEngine, PlayerRoleLoader roleLoader) {
        this.ballEngine = ballEngine;
        this.roleLoader = roleLoader;
    }

    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxBalls,
            Integer target
    ) {
        return simulateInnings(battingOrder, bowlingOrder, maxBalls, target,
                null, 0, 0, null);
    }

    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxBalls,
            Integer target,
            DeclarationEngine declarationEngine,
            int inningsNumber,
            int firstInningsLead
    ) {
        return simulateInnings(battingOrder, bowlingOrder, maxBalls, target,
                declarationEngine, inningsNumber, firstInningsLead, null);
    }

    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxBalls,
            Integer target,
            DeclarationEngine declarationEngine,
            int inningsNumber,
            int firstInningsLead,
            BowlingPlan bowlingPlan
    ) {
        int totalRuns = 0;
        int wickets   = 0;
        int balls     = 0;

        int strikerIndex    = 0;
        int nonStrikerIndex = 1;
        int nextBatterIndex = 2;

        int bowlerIndex = 0;  // fallback only

        // ── Scorecard tracking ────────────────────────────────────────────
        Map<String, BatterRecord>  batRecords  = new LinkedHashMap<>();
        Map<String, BowlerRecord>  bowlRecords = new LinkedHashMap<>();
        List<FallOfWicket>         fow         = new ArrayList<>();
        List<Partnership>          partnerships = new ArrayList<>();

        for (String name : battingOrder) batRecords.put(name, new BatterRecord(name));

        // Current partnership
        Partnership currentPartnership = new Partnership(
                battingOrder.get(0), battingOrder.get(1));

        // Declaration batter scores
        Map<String, Integer> declarationScores = new HashMap<>();
        for (String b : battingOrder) declarationScores.put(b, 0);

        boolean declared = false;

        while (wickets < 10 && balls < maxBalls) {

            String striker    = battingOrder.get(strikerIndex);
            int currentOver   = (balls / 6) + 1; // 1-based over number
            String bowler     = (bowlingPlan != null && bowlingPlan.getAssignment(currentOver) != null)
                    ? bowlingPlan.getAssignment(currentOver)
                    : bowlingOrder.get(bowlerIndex % bowlingOrder.size());
            String bowlRole   = roleLoader.getBowlRole(bowler);
            String batterHand = roleLoader.getBatRole(striker);

            if (bowlRole   == null || bowlRole.isBlank())   bowlRole   = "RF";
            if (batterHand == null || batterHand.isBlank()) batterHand = "RHB";

            boolean isTail = strikerIndex >= 7;

            // Declaration check
            if (declarationEngine != null && target == null) {
                double inningsOvers   = balls / 6.0;
                double remainingOvers = (maxBalls - balls) / 6.0;
                int lead = totalRuns + firstInningsLead;
                if (declarationEngine.shouldDeclare(
                        inningsNumber, totalRuns, inningsOvers,
                        lead, remainingOvers, declarationScores)) {
                    declared = true;
                    break;
                }
            }

            BallOutcome outcome = ballEngine.simulateBall(
                    striker, bowler, bowlRole, batterHand);

            balls++;

            // Ensure bowler record exists
            bowlRecords.computeIfAbsent(bowler, BowlerRecord::new);
            BatterRecord batRec  = batRecords.get(striker);
            BowlerRecord bowlRec = bowlRecords.get(bowler);

            if (outcome.isWicket()) {
                wickets++;
                batRec.record(0, true);
                batRec.dismissalInfo = "b " + bowler;
                bowlRec.record(0, true);

                // Partnership ends — count the wicket ball
                currentPartnership.record(0);
                partnerships.add(currentPartnership);

                // Fall of wicket
                fow.add(new FallOfWicket(wickets, striker, totalRuns, balls));

                // Last wicket fragility
                if (wickets == 9 && random.nextDouble() < 0.15) break;

                if (nextBatterIndex < battingOrder.size()) {
                    // New batter comes in — start new partnership
                    String newBatter = battingOrder.get(nextBatterIndex);
                    String otherBatter = battingOrder.get(nonStrikerIndex);
                    currentPartnership = new Partnership(newBatter, otherBatter);

                    strikerIndex = nextBatterIndex;
                    nextBatterIndex++;
                } else {
                    break;
                }

            } else {
                int runs = outcome.getRuns();

                if (isTail && runs >= 4 && random.nextDouble() < 0.25) runs = 1;

                totalRuns += runs;
                batRec.record(runs, false);
                bowlRec.record(runs, false);
                currentPartnership.record(runs);
                declarationScores.merge(striker, runs, Integer::sum);

                if (target != null && totalRuns >= target) break;

                if (runs % 2 == 1) {
                    int temp = strikerIndex;
                    strikerIndex    = nonStrikerIndex;
                    nonStrikerIndex = temp;
                    // Don't recreate partnership — just swap ends
                }
            }

            // End of over — swap ends
            if (balls % 6 == 0) {
                int temp = strikerIndex;
                strikerIndex    = nonStrikerIndex;
                nonStrikerIndex = temp;

                // Fallback rotation if no plan
                if (bowlingPlan == null) {
                    bowlerIndex = (bowlerIndex + 1) % bowlingOrder.size();
                }
            }
        }

        // Close unfinished partnership
        if (wickets < 10 || declared) {
            partnerships.add(currentPartnership);
        }

        // Build ordered batting/bowling cards
        List<BatterRecord> battingCard = new ArrayList<>();
        for (String name : battingOrder) {
            BatterRecord rec = batRecords.get(name);
            if (rec != null && (rec.balls > 0 || battingOrder.indexOf(name) <= wickets + 1)) {
                battingCard.add(rec);
            }
        }

        List<BowlerRecord> bowlingCard = new ArrayList<>(bowlRecords.values());

        return new InningsResult(totalRuns, wickets, balls, declared,
                battingCard, bowlingCard, fow, partnerships);
    }

    public void setPitch(PitchProfile pitch) {
        ballEngine.setPitch(pitch);
    }
}