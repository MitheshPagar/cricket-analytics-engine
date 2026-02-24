package com.cricket.engine;

import java.util.List;

public class TestMatchEngine {

    private final InningsEngine inningsEngine;
    private final DeterioratingPitch pitch;

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

        // =====================
        // 1st Innings
        // =====================
        System.out.println("\n--- 1st Innings: "+ teamAName +" ---");

        inningsEngine.setPitch(pitch.currentProfile());
        InningsResult aFirst =
                inningsEngine.simulateInnings(teamA, teamB, 90);

        a1 = aFirst.getRuns();
        System.out.println(aFirst);

        pitch.deteriorate();

        // =====================
        // 2nd Innings
        // =====================
        System.out.println("\n--- 2nd Innings:"+ teamBName +" ---");

        inningsEngine.setPitch(pitch.currentProfile());
        InningsResult bFirst =
                inningsEngine.simulateInnings(teamB, teamA, 90);

        b1 = bFirst.getRuns();
        System.out.println(bFirst);

        pitch.deteriorate();

        int lead = a1 - b1;

        // =====================
        // Follow-On Check
        // =====================
        boolean followOn = lead >= 200;

        if (followOn) {
            System.out.println("\nFollow-on enforced!");

            System.out.println("\n--- 3rd Innings: "+ teamBName +" ---");

            inningsEngine.setPitch(pitch.currentProfile());
            InningsResult bSecond =
                    inningsEngine.simulateInnings(teamB, teamA, 90);

            b2 = bSecond.getRuns();
            System.out.println(bSecond);

            pitch.deteriorate();

            int target = (a1 - b1 - b2) + 1;

            if (target <= 0) {
                System.out.println("\n" + teamBName + " wins by innings and " + (-target) + " runs!");
                return;
            }

            System.out.println("\n--- 4th Innings: "+teamAName+ " (Chasing "
                    + target + ") ---");

            inningsEngine.setPitch(pitch.currentProfile());
            InningsResult aSecond =
                    inningsEngine.simulateInnings(teamA, teamB, 90);

            a2 = aSecond.getRuns();
            System.out.println(aSecond);

            printChaseResult(a2, target, teamAName);

        } else {

            // =====================
            // Normal 3rd Innings
            // =====================
            System.out.println("\n--- 3rd Innings: "+ teamAName +" ---");

            inningsEngine.setPitch(pitch.currentProfile());
            InningsResult aSecond =
                    inningsEngine.simulateInnings(teamA, teamB, 90);

            a2 = aSecond.getRuns();
            System.out.println(aSecond);

            pitch.deteriorate();

            int target = (a1 + a2 - b1) + 1;

            System.out.println("\n--- 4th Innings: "+teamBName+" (Chasing "
                    + target + ") ---");

            inningsEngine.setPitch(pitch.currentProfile());
            InningsResult bSecond =
                    inningsEngine.simulateInnings(teamB, teamA, 90);

            b2 = bSecond.getRuns();
            System.out.println(bSecond);

            printChaseResult(b2, target, teamBName);
        }
    }

    private void printChaseResult(int runs,
                                  int target,
                                  String chasingTeam) {

        System.out.println("\n============================");

        if (runs >= target) {
            System.out.println(chasingTeam
                    + " wins by wickets!");
        } else if (runs < target) {
            System.out.println("Opponent wins by "
                    + (target - runs - 1) + " runs");
        } else {
            System.out.println("Match Drawn");
        }

        System.out.println("============================");
    }
}