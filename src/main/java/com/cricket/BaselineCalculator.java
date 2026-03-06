package com.cricket;

import java.util.HashMap;
import java.util.Map;

public class BaselineCalculator {

    private final Map<String, Double> roleRunsPerBall = new HashMap<>();
    private final Map<String, Double> roleWicketsPerBall = new HashMap<>();

    private double lhbRunsPerBall = 0.0;
    private double lhbWicketsPerBall = 0.0;

    private double rhbRunsPerBall = 0.0;
    private double rhbWicketsPerBall = 0.0;

    // Single authoritative WPB baseline derived from batter dismissal data
    private double overallWicketsPerBall = 0.02;

    public void compute(
            Map<String, Map<String, Stats>> batterStats,
            Map<String, Map<String, Stats>> bowlerStats
    ) {

        Map<String, Long> roleTotalBalls = new HashMap<>();
        Map<String, Long> roleTotalRuns = new HashMap<>();
        Map<String, Long> roleTotalWickets = new HashMap<>();

        final int MIN_BALLS_FOR_BASELINE = 200; // exclude tiny samples from baseline

        for (Map<String, Stats> playerMap : batterStats.values()) {
            for (Map.Entry<String, Stats> entry : playerMap.entrySet()) {

                String role = entry.getKey();
                Stats stats = entry.getValue();

                // Only include players with enough data to be meaningful
                if (stats.getBalls() < MIN_BALLS_FOR_BASELINE) continue;

                roleTotalBalls.merge(role, (long) stats.getBalls(), Long::sum);
                roleTotalRuns.merge(role, (long) stats.getRuns(), Long::sum);
                roleTotalWickets.merge(role, (long) stats.getDismissals(), Long::sum);
            }
        }

        for (String role : roleTotalBalls.keySet()) {
            long balls = roleTotalBalls.getOrDefault(role, 0L);
            long runs = roleTotalRuns.getOrDefault(role, 0L);
            long wickets = roleTotalWickets.getOrDefault(role, 0L);

            if (balls > 0) {
                roleRunsPerBall.put(role, (double) runs / balls);
                roleWicketsPerBall.put(role, (double) wickets / balls);
            } else {
                roleRunsPerBall.put(role, 0.0);
                roleWicketsPerBall.put(role, 0.0);
            }
        }

        long totalLHBBalls = 0;
        long totalLHBRuns = 0;
        long totalLHBWickets = 0;

        long totalRHBBalls = 0;
        long totalRHBRuns = 0;
        long totalRHBWickets = 0;

        for (Map<String, Stats> bowlerMap : bowlerStats.values()) {

            Stats vsLHB = bowlerMap.get("LHB");
            if (vsLHB != null && vsLHB.getBalls() >= MIN_BALLS_FOR_BASELINE) {
                totalLHBBalls += vsLHB.getBalls();
                totalLHBRuns += vsLHB.getRuns();
                totalLHBWickets += vsLHB.getDismissals();
            }

            Stats vsRHB = bowlerMap.get("RHB");
            if (vsRHB != null && vsRHB.getBalls() >= MIN_BALLS_FOR_BASELINE) {
                totalRHBBalls += vsRHB.getBalls();
                totalRHBRuns += vsRHB.getRuns();
                totalRHBWickets += vsRHB.getDismissals();
            }
        }

        if (totalLHBBalls > 0) {
            lhbRunsPerBall = (double) totalLHBRuns / totalLHBBalls;
            lhbWicketsPerBall = (double) totalLHBWickets / totalLHBBalls;
        }

        if (totalRHBBalls > 0) {
            rhbRunsPerBall = (double) totalRHBRuns / totalRHBBalls;
            rhbWicketsPerBall = (double) totalRHBWickets / totalRHBBalls;
        }

        // Compute single overall WPB from all batter dismissal data
        long totalBalls = 0, totalDismissals = 0;
        for (Map<String, Stats> playerMap : batterStats.values()) {
            for (Stats s : playerMap.values()) {
                if (s.getBalls() >= MIN_BALLS_FOR_BASELINE) {
                    totalBalls      += s.getBalls();
                    totalDismissals += s.getDismissals();
                }
            }
        }
        if (totalBalls > 0) overallWicketsPerBall = (double) totalDismissals / totalBalls;

        System.out.println("Baselines computed successfully");
        System.out.println(String.format(
            "  Overall WPB=%.5f | LHB RPB=%.4f | RHB RPB=%.4f",
            overallWicketsPerBall, lhbRunsPerBall, rhbRunsPerBall));
    }


    public double getBaselineRunsPerBallForRole(String role) {
        return roleRunsPerBall.getOrDefault(role, 0.7); // safe fallback
    }

    public double getBaselineWicketsPerBallForRole(String role) {
        return roleWicketsPerBall.getOrDefault(role, 0.02); // safe fallback
    }

    public double getLhbRunsPerBall() {
        return lhbRunsPerBall;
    }

    public double getLhbWicketsPerBall() {
        return lhbWicketsPerBall;
    }

    public double getRhbRunsPerBall() {
        return rhbRunsPerBall;
    }

    public double getRhbWicketsPerBall() {
        return rhbWicketsPerBall;
    }

    public double getOverallWicketsPerBall() {
        return overallWicketsPerBall;
    }
}