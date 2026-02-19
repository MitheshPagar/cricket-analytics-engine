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

    public void compute(
            Map<String, Map<String, Stats>> batterStats,
            Map<String, Map<String, Stats>> bowlerStats
    ) {

        Map<String, Long> roleTotalBalls = new HashMap<>();
        Map<String, Long> roleTotalRuns = new HashMap<>();
        Map<String, Long> roleTotalWickets = new HashMap<>();

        for (Map<String, Stats> playerMap : batterStats.values()) {
            for (Map.Entry<String, Stats> entry : playerMap.entrySet()) {

                String role = entry.getKey();
                Stats stats = entry.getValue();

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
            if (vsLHB != null) {
                totalLHBBalls += vsLHB.getBalls();
                totalLHBRuns += vsLHB.getRuns();
                totalLHBWickets += vsLHB.getDismissals();
            }

            Stats vsRHB = bowlerMap.get("RHB");
            if (vsRHB != null) {
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

        System.out.println("Baselines computed successfully");
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
}
