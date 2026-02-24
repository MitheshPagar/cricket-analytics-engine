package com.cricket.engine;

import java.util.List;
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

    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxOvers,
            Integer target
    ) {

        int totalRuns = 0;
        int wickets = 0;
        int balls = 0;

        int strikerIndex = 0;
        int nonStrikerIndex = 1;
        int nextBatterIndex = 2;

        int bowlerIndex = 0;
        int oversByCurrentBowler = 0;

        while (wickets < 10 && balls < maxOvers * 6) {

            String striker = battingOrder.get(strikerIndex);
            String bowler = bowlingOrder.get(bowlerIndex);

            String bowlRole = roleLoader.getBowlRole(bowler);
            String batterHand = roleLoader.getBatRole(striker);

            // Fallback if role missing
            if (bowlRole == null) bowlRole = "RF";
            if (batterHand == null) batterHand = "RHB";

            boolean isTail = strikerIndex >= 7;

            BallOutcome outcome = ballEngine.simulateBall(
                    striker,
                    bowler,
                    bowlRole,
                    batterHand
            );

            balls++;

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

                oversByCurrentBowler++;

                // Swap strike at end of over
                int temp = strikerIndex;
                strikerIndex = nonStrikerIndex;
                nonStrikerIndex = temp;

                // 5-over spell rotation
                if (oversByCurrentBowler >= 5) {
                    bowlerIndex =
                            (bowlerIndex + 1) % bowlingOrder.size();
                    oversByCurrentBowler = 0;
                }
            }
        }

        return new InningsResult(totalRuns, wickets, balls);
    }

    public void setPitch(PitchProfile pitch) {
        ballEngine.setPitch(pitch);
    }
}