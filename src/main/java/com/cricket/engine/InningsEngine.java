package com.cricket.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.cricket.PlayerRoleLoader;

public class InningsEngine {

    private final BallEngine ballEngine;
    private final PlayerRoleLoader roleLoader;
    private final Random random = new Random();

    public InningsEngine(BallEngine ballEngine,
                         PlayerRoleLoader roleLoader) {
        this.ballEngine = ballEngine;
        this.roleLoader = roleLoader;
    }

    /**
     * Simulate an innings without declaration support (e.g. 4th innings / chases).
     */
    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxBalls,
            Integer target
    ) {
        return simulateInnings(battingOrder, bowlingOrder, maxBalls, target,
                null, 0, 0);
    }

    /**
     * Simulate an innings with optional declaration logic.
     *
     * @param battingOrder      batting lineup
     * @param bowlingOrder      bowling lineup
     * @param maxBalls          max balls allowed (match time limit)
     * @param target            run target to chase (null if not chasing)
     * @param declarationEngine declaration engine to poll each ball (null = no declaration)
     * @param inningsNumber     1-based innings number in the match
     * @param firstInningsLead  runs scored in batting team's first innings minus
     *                          opposition first innings (used to compute live lead).
     *                          Pass 0 for innings 1 and 2 where lead = current runs.
     */
    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxBalls,
            Integer target,
            DeclarationEngine declarationEngine,
            int inningsNumber,
            int firstInningsLead
    ) {

        int totalRuns = 0;
        int wickets = 0;
        int balls = 0;

        int strikerIndex = 0;
        int nonStrikerIndex = 1;
        int nextBatterIndex = 2;

        int bowlerIndex = 0;
        int spellBalls = 0;

        // Per-batter score tracking for declaration courtesy rule
        Map<String, Integer> batterScores = new HashMap<>();
        for (String batter : battingOrder) {
            batterScores.put(batter, 0);
        }

        boolean declared = false;

        while (wickets < 10 && balls < maxBalls) {

            String striker = battingOrder.get(strikerIndex);
            String bowler = bowlingOrder.get(bowlerIndex);

            String bowlRole = roleLoader.getBowlRole(bowler);
            String batterHand = roleLoader.getBatRole(striker);

            // Fallback if role missing
            if (bowlRole == null || bowlRole.isBlank()) bowlRole = "RF";
            if (batterHand == null || batterHand.isBlank()) batterHand = "RHB";

            boolean isTail = strikerIndex >= 7;

            // --- Declaration check (before each ball) ---
            if (declarationEngine != null && target == null) {

                double inningsOvers = balls / 6.0;
                double remainingOvers = (maxBalls - balls) / 6.0;

                // Lead = first innings lead baseline + runs scored this innings
                // For innings 1: firstInningsLead=0, lead just equals totalRuns
                // For innings 2: firstInningsLead = -(opposition 1st innings), lead = totalRuns + firstInningsLead
                // For innings 3: firstInningsLead = a1 - b1, lead = totalRuns + firstInningsLead
                int lead = totalRuns + firstInningsLead;

                if (declarationEngine.shouldDeclare(
                        inningsNumber,
                        totalRuns,
                        inningsOvers,
                        lead,
                        remainingOvers,
                        batterScores
                )) {
                    declared = true;
                    break;
                }
            }

            BallOutcome outcome = ballEngine.simulateBall(
                    striker,
                    bowler,
                    bowlRole,
                    batterHand
            );

            balls++;
            spellBalls++;

            if (outcome.isWicket()) {

                wickets++;

                // Last wicket fragility
                if (wickets == 9 && random.nextDouble() < 0.15) {
                    break;
                }

                if (nextBatterIndex < battingOrder.size()) {
                    strikerIndex = nextBatterIndex;
                    nextBatterIndex++;
                } else {
                    break;
                }

            } else {

                int runs = outcome.getRuns();

                // Tail nerf: reduce big hitting reliability
                if (isTail && runs >= 4 && random.nextDouble() < 0.25) {
                    runs = 1;
                }

                totalRuns += runs;

                // Update individual batter score
                batterScores.merge(striker, runs, Integer::sum);

                if (target != null && totalRuns >= target) {
                    break;
                }

                // Strike rotation for odd runs
                if (runs % 2 == 1) {
                    int temp = strikerIndex;
                    strikerIndex = nonStrikerIndex;
                    nonStrikerIndex = temp;
                }
            }

            // End of over logic
            if (balls % 6 == 0) {

                // Swap strike at end of over
                int temp = strikerIndex;
                strikerIndex = nonStrikerIndex;
                nonStrikerIndex = temp;

                // 5-over spell rotation
                if (spellBalls >= 30) {
                    bowlerIndex =
                            (bowlerIndex + 1) % bowlingOrder.size();
                    spellBalls = 0;
                }
            }
        }

        return new InningsResult(totalRuns, wickets, balls, declared);
    }

    public void setPitch(PitchProfile pitch) {
        ballEngine.setPitch(pitch);
    }
}