package com.cricket.engine;

import java.util.List;

import com.cricket.PlayerRoleLoader;

public class InningsEngine {

    private final BallEngine ballEngine;
    private final PlayerRoleLoader roleLoader;

    public InningsEngine(BallEngine ballEngine, PlayerRoleLoader roleLoader) {
        this.ballEngine = ballEngine;
        this.roleLoader = roleLoader;
    }

    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            int maxOvers
    ) {

        int totalRuns = 0;
        int wickets = 0;
        int balls = 0;

        int strikerIndex = 0;
        int nonStrikerIndex = 1;
        int nextBatterIndex = 2;

        int bowlerIndex = 0;

        while (wickets < 10 && balls < maxOvers * 6) {

            String striker = battingOrder.get(strikerIndex);
            String bowler = bowlingOrder.get(bowlerIndex);

            String bowlRole = roleLoader.getBowlRole(bowler);
            String batterHand = roleLoader.getBatRole(striker);

            if (bowlRole == null || batterHand == null) {
                // Skip unrealistic matchup
                balls++;
                continue;
            }

            BallOutcome outcome = ballEngine.simulateBall(
                    striker,
                    bowler,
                    bowlRole,
                    batterHand
            );

            balls++;

            if (outcome.isWicket()) {

                wickets++;

                if (nextBatterIndex < battingOrder.size()) {
                    strikerIndex = nextBatterIndex;
                    nextBatterIndex++;
                } else {
                    break;
                }

            } else {

                int runs = outcome.getRuns();
                totalRuns += runs;

                // Strike rotation for odd runs
                if (runs % 2 == 1) {
                    int temp = strikerIndex;
                    strikerIndex = nonStrikerIndex;
                    nonStrikerIndex = temp;
                }
            }

            // End of over logic
            if (balls % 6 == 0) {

                // Swap strike
                int temp = strikerIndex;
                strikerIndex = nonStrikerIndex;
                nonStrikerIndex = temp;

                // Rotate bowler
                bowlerIndex = (bowlerIndex + 1) % bowlingOrder.size();
            }
        }

        return new InningsResult(totalRuns, wickets, balls);
    }
}