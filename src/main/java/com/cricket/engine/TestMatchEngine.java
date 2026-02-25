package com.cricket.engine;

import java.util.List;

public class TestMatchEngine {

    private final InningsEngine inningsEngine;
    private final DeterioratingPitch pitch;

    private int matchBalls = 0;
    private static final int MAX_MATCH_BALLS = 450 * 6;

    public TestMatchEngine(InningsEngine inningsEngine,
                           PitchProfile basePitch) {

        this.inningsEngine = inningsEngine;
        this.pitch = new DeterioratingPitch(basePitch);
    }

    public void simulateMatch(String teamAName,
                              List<String> teamA,
                              String teamBName,
                              List<String> teamB) {

        int a1, b1, a2 = 0, b2 = 0;

        // 1st Innings
        System.out.println("\n--- 1st Innings: " + teamAName + " ---");
        InningsResult aFirst =
                playInnings(teamA, teamB, null);
        a1 = aFirst.getRuns();
        System.out.println(aFirst);

        if (timeExpired()) return;

        // 2nd Innings
        System.out.println("\n--- 2nd Innings: " + teamBName + " ---");
        InningsResult bFirst =
                playInnings(teamB, teamA, null);
        b1 = bFirst.getRuns();
        System.out.println(bFirst);

        if (timeExpired()) return;

        int lead = a1 - b1;
        boolean followOn = lead >= 200;

        if (followOn) {

            System.out.println("\nFollow-on enforced!");

            System.out.println("\n--- 3rd Innings: " + teamBName + " ---");
            InningsResult bSecond =
                    playInnings(teamB, teamA, null);
            b2 = bSecond.getRuns();
            System.out.println(bSecond);

            if (timeExpired()) return;

            int target = (a1 - b1 - b2) + 1;

            if (target <= 0) {
                System.out.println("\n" + teamAName + " wins by an innings!");
                return;
            }

            System.out.println("\n--- 4th Innings: " + teamAName
                    + " (Chasing " + target + ") ---");

            InningsResult aSecond =
                    playInnings(teamA, teamB, target);

            System.out.println(aSecond);

            printChaseResult(aSecond, target,
                    teamAName, teamBName);

        } else {

            System.out.println("\n--- 3rd Innings: " + teamAName + " ---");
            InningsResult aSecond =
                    playInnings(teamA, teamB, null);
            a2 = aSecond.getRuns();
            System.out.println(aSecond);

            if (timeExpired()) return;

            int target = (a1 + a2 - b1) + 1;

            System.out.println("\n--- 4th Innings: " + teamBName
                    + " (Chasing " + target + ") ---");

            InningsResult bSecond =
                    playInnings(teamB, teamA, target);

            System.out.println(bSecond);

            printChaseResult(bSecond, target,
                    teamBName, teamAName);
        }
    }

    private InningsResult playInnings(
            List<String> batting,
            List<String> bowling,
            Integer target
    ) {

        inningsEngine.setPitch(pitch.currentProfile());

        int remainingBalls = MAX_MATCH_BALLS - matchBalls;

        InningsResult result =
                inningsEngine.simulateInnings(
                        batting,
                        bowling,
                        remainingBalls,
                        target
                );

        matchBalls += result.getBalls();

        pitch.deteriorate();

        return result;
    }

    private boolean timeExpired() {
        if (matchBalls >= MAX_MATCH_BALLS) {
            System.out.println("\nMatch Drawn");
            return true;
        }
        return false;
    }

    private void printChaseResult(InningsResult result,
                                  int target,
                                  String chasingTeam,
                                  String defendingTeam) {

        System.out.println("\n============================");

        int runs = result.getRuns();
        int wickets = result.getWickets();

        if (runs >= target) {
            int wicketsRemaining = 10 - wickets;
            System.out.println(chasingTeam
                    + " wins by "
                    + wicketsRemaining
                    + " wickets");
        } else {
            int margin = target - runs - 1;
            System.out.println(defendingTeam
                    + " wins by "
                    + margin
                    + " runs");
        }

        System.out.println("============================");
    }
}