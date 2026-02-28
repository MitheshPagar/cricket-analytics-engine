package com.cricket.engine;

import java.util.List;

public class TestMatchEngine {

    private final InningsEngine inningsEngine;
    private final DeclarationEngine declarationEngine;
    private final DeterioratingPitch pitch;

    private int matchBalls = 0;
    private static final int MAX_MATCH_BALLS = 450 * 6;

    // Bowling plans set before match runs (optional — null = use full XI)
    private BowlingPlan teamABowlingPlan = null;
    private BowlingPlan teamBBowlingPlan = null;

    public TestMatchEngine(InningsEngine inningsEngine,
                           PitchProfile basePitch) {
        this.inningsEngine = inningsEngine;
        this.declarationEngine = new DeclarationEngine();
        this.pitch = new DeterioratingPitch(basePitch);
    }

    // ── Called from GUI via MatchLauncher ────────────────────────────────────
    public void simulateMatch(String teamAName, List<String> teamA,
                              String teamBName, List<String> teamB,
                              BowlingPlan teamABowlingPlan,
                              BowlingPlan teamBBowlingPlan) {
        this.teamABowlingPlan = teamABowlingPlan;
        this.teamBBowlingPlan = teamBBowlingPlan;
        simulateMatch(teamAName, teamA, teamBName, teamB);
    }

    // ── Core match flow ──────────────────────────────────────────────────────
    public void simulateMatch(String teamAName, List<String> teamA,
                              String teamBName, List<String> teamB) {
        int a1, b1, a2 = 0, b2 = 0;

        // 1st Innings: Team A bat, Team B bowl
        System.out.println("\n--- 1st Innings: " + teamAName + " ---");
        InningsResult aFirst = playInnings(teamA, teamB, null, 1, 0, teamBBowlingPlan);
        a1 = aFirst.getRuns();
        System.out.println(aFirst);
        if (aFirst.isDeclared()) System.out.println(teamAName + " have declared!");

        if (timeExpired()) return;

        // 2nd Innings: Team B bat, Team A bowl
        System.out.println("\n--- 2nd Innings: " + teamBName + " ---");
        InningsResult bFirst = playInnings(teamB, teamA, null, 2, -a1, teamABowlingPlan);
        b1 = bFirst.getRuns();
        System.out.println(bFirst);
        if (bFirst.isDeclared()) System.out.println(teamBName + " have declared!");

        if (timeExpired()) return;

        int lead = a1 - b1;
        boolean followOn = lead >= 200;

        if (followOn) {

            System.out.println("\nFollow-on enforced! "
                    + teamAName + " lead by " + lead + " runs.");

            // 3rd Innings: Team B follow on, Team A bowl
            System.out.println("\n--- 3rd Innings: " + teamBName + " (following on) ---");
            InningsResult bSecond = playInnings(teamB, teamA, null, 3,
                    b1 - a1, teamABowlingPlan);
            b2 = bSecond.getRuns();
            System.out.println(bSecond);
            if (bSecond.isDeclared()) System.out.println(teamBName + " have declared!");

            if (timeExpired()) return;

            if (b1 + b2 > a1) {
                int bLead     = (b1 + b2) - a1;
                int target4th = bLead + 1;
                System.out.println("\n--- 4th Innings: " + teamAName
                        + " (Chasing " + target4th + ") ---");
                InningsResult aSecond = playInnings(teamA, teamB, target4th,
                        4, 0, teamBBowlingPlan);
                System.out.println(aSecond);
                printChaseResult(aSecond, target4th, teamAName, teamBName);
                return;
            }

            int target = (a1 - b1 - b2) + 1;
            if (target <= 0) {
                System.out.println("\n" + teamAName + " wins by an innings and "
                        + Math.abs(target - 1) + " runs!");
                return;
            }

            System.out.println("\n--- 4th Innings: " + teamAName
                    + " (Chasing " + target + ") ---");
            InningsResult aSecond = playInnings(teamA, teamB, target, 4, 0,
                    teamBBowlingPlan);
            System.out.println(aSecond);
            printChaseResult(aSecond, target, teamAName, teamBName);

        } else {

            // 3rd Innings: Team A bat, Team B bowl
            System.out.println("\n--- 3rd Innings: " + teamAName + " ---");
            InningsResult aSecond = playInnings(teamA, teamB, null, 3,
                    a1 - b1, teamBBowlingPlan);
            a2 = aSecond.getRuns();
            System.out.println(aSecond);
            if (aSecond.isDeclared()) System.out.println(teamAName + " have declared!");

            if (timeExpired()) return;

            int target = (a1 + a2 - b1) + 1;

            System.out.println("\n--- 4th Innings: " + teamBName
                    + " (Chasing " + target + ") ---");
            InningsResult bSecond = playInnings(teamB, teamA, target, 4, 0,
                    teamABowlingPlan);
            b2 = bSecond.getRuns();
            System.out.println(bSecond);
            printChaseResult(bSecond, target, teamBName, teamAName);
        }
    }

    // ── Internal innings runner ──────────────────────────────────────────────
    private InningsResult playInnings(List<String> batting,
                                      List<String> bowling,
                                      Integer target,
                                      int inningsNumber,
                                      int firstInningsLead,
                                      BowlingPlan bowlingPlan) {

        inningsEngine.setPitch(pitch.currentProfile());
        int remainingBalls = MAX_MATCH_BALLS - matchBalls;

        // Use GUI bowling plan if provided, else fall back to full XI list
        List<String> bowlingOrder = (bowlingPlan != null)
                ? bowlingPlan.toOrderedBowlingList(remainingBalls / 6)
                : bowling;

        if (bowlingOrder == null || bowlingOrder.isEmpty()) {
            bowlingOrder = bowling;
        }

        DeclarationEngine decEngine = (inningsNumber < 4) ? declarationEngine : null;

        InningsResult result = inningsEngine.simulateInnings(
                batting, bowlingOrder, remainingBalls,
                target, decEngine, inningsNumber, firstInningsLead
        );

        matchBalls += result.getBalls();
        pitch.deteriorate();
        return result;
    }

    private boolean timeExpired() {
        if (matchBalls >= MAX_MATCH_BALLS) {
            System.out.println("\nMatch Drawn (time expired)");
            return true;
        }
        return false;
    }

    private void printChaseResult(InningsResult result, int target,
                                  String chasingTeam, String defendingTeam) {
        System.out.println("\n============================");
        int runs    = result.getRuns();
        int wickets = result.getWickets();

        if (runs >= target) {
            System.out.println(chasingTeam + " wins by " + (10 - wickets) + " wickets");
        } else if (matchBalls >= MAX_MATCH_BALLS) {
            System.out.println("Match Drawn");
        } else {
            System.out.println(defendingTeam + " wins by " + (target - runs - 1) + " runs");
        }
        System.out.println("============================");
    }
}