package com.cricket.engine;

import java.util.List;

public class InningsEngine {

    private final BallEngine ballEngine;

    public InningsEngine(BallEngine ballEngine) {
        this.ballEngine = ballEngine;
    }

    public InningsResult simulateInnings(
            List<String> battingOrder,
            List<String> bowlingOrder,
            String bowlRole,
            String strikerHand,
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

            BallOutcome outcome = ballEngine.simulateBall(
                    striker,
                    bowler,
                    bowlRole,
                    strikerHand
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


                if (runs % 2 == 1) {
                    int temp = strikerIndex;
                    strikerIndex = nonStrikerIndex;
                    nonStrikerIndex = temp;
                }
            }


            if (balls % 6 == 0) {


                int temp = strikerIndex;
                strikerIndex = nonStrikerIndex;
                nonStrikerIndex = temp;

                bowlerIndex = (bowlerIndex + 1) % bowlingOrder.size();
            }
        }

        return new InningsResult(totalRuns, wickets, balls);
    }
}