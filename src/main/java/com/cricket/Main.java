package com.cricket;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    private static final String[] ROLE_ORDER = {
            "RF", "LF",
            "RFM", "LFM",
            "RMF", "LMF",
            "RM", "LM",
            "ROS", "LOS",
            "RLS", "LLS"
    };

    public static void main(String[] args) throws Exception {

        System.out.println("Starting Cricket Stats Engine...");

        
        PlayerRoleLoader roleLoader = new PlayerRoleLoader();
        roleLoader.load(com.cricket.engine.PathResolver.resolve("playerRoles.csv"));
        System.out.println("Player roles loaded");

        ObjectMapper mapper = new ObjectMapper();

        
        Map<String, Map<String, Stats>> batterStats = new HashMap<>();
        Map<String, Map<String, Stats>> bowlerStats = new HashMap<>();
        BaselineCalculator baselineCalculator = new BaselineCalculator();

        
        Files.list(com.cricket.engine.PathResolver.resolvePath("matches"))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> processMatch(
                        path.toFile(),
                        mapper,
                        roleLoader,
                        batterStats,
                        bowlerStats
                ));

        System.out.println("Aggregation complete");
        System.out.println("Total batters tracked: " + batterStats.size());
        System.out.println("Total bowlers tracked: " + bowlerStats.size());

        // ── Nerf diagnostic: print sample players to verify shrinkage ───────
        System.out.println("\n── Nerf Diagnostic (sample) ──────────────────────");
        batterStats.entrySet().stream().limit(5).forEach(e -> {
            e.getValue().forEach((role, s) -> {
                double bRPB = baselineCalculator.getBaselineRunsPerBallForRole(role);
                double bWPB = baselineCalculator.getBaselineWicketsPerBallForRole(role);
                System.out.println("BAT " + e.getKey() + " vs " + role + ": " + s.debugNerf(bRPB, bWPB));
            });
        });
        bowlerStats.entrySet().stream().limit(5).forEach(e -> {
            e.getValue().forEach((hand, s) -> {
                double bRPB = hand.equals("LHB") ? baselineCalculator.getLhbRunsPerBall() : baselineCalculator.getRhbRunsPerBall();
                double bWPB = hand.equals("LHB") ? baselineCalculator.getLhbWicketsPerBall() : baselineCalculator.getRhbWicketsPerBall();
                System.out.println("BOWL " + e.getKey() + " vs " + hand + ": " + s.debugNerf(bRPB, bWPB));
            });
        });
        System.out.println("──────────────────────────────────────────────────\n");

        
        System.out.println("Computing baselines...");
        baselineCalculator.compute(batterStats, bowlerStats);
        System.out.println("Baselines ready for simulation");

        
        exportSimSheet(batterStats, bowlerStats, baselineCalculator);

        System.out.println("Pipeline completed successfully");
    }

    
    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    private static void processMatch(
            File file,
            ObjectMapper mapper,
            PlayerRoleLoader roleLoader,
            Map<String, Map<String, Stats>> batterStats,
            Map<String, Map<String, Stats>> bowlerStats
    ) {

        try {
            JsonNode root = mapper.readTree(file);
            JsonNode innings = root.get("innings");

            for (JsonNode inning : innings) {
                for (JsonNode over : inning.get("overs")) {
                    for (JsonNode delivery : over.get("deliveries")) {

                        String batter = delivery.get("batter").asText();
                        String bowler = delivery.get("bowler").asText();

                        String bowlRole = roleLoader.getBowlRole(bowler);
                        String batterHand = roleLoader.getBatRole(batter);

                        
                        boolean isWide = delivery.has("extras")
                                && delivery.get("extras").has("wides");

                        if (isWide) continue;

                        int batterRuns = delivery.get("runs").get("batter").asInt();
                        int totalRuns = delivery.get("runs").get("total").asInt();

                        boolean isWicket = false;
                        if (delivery.has("wickets")) {
                            for (JsonNode wicket : delivery.get("wickets")) {
                                String outPlayer = wicket.get("player_out").asText();
                                if (outPlayer.equals(batter)) {
                                    isWicket = true;
                                }
                            }
                        }

                        
                        if (bowlRole != null && !bowlRole.isBlank()) {

                            batterStats
                                    .computeIfAbsent(batter, k -> new HashMap<>())
                                    .computeIfAbsent(bowlRole, k -> new Stats());

                            Stats batStats = batterStats.get(batter).get(bowlRole);
                            batStats.recordBall(batterRuns, isWicket);
                        }

                        
                        if (batterHand != null && !batterHand.isBlank()) {

                            bowlerStats
                                    .computeIfAbsent(bowler, k -> new HashMap<>())
                                    .computeIfAbsent(batterHand, k -> new Stats());

                            Stats bowlStats = bowlerStats.get(bowler).get(batterHand);
                            bowlStats.recordBall(totalRuns, isWicket);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing match: " + file.getName());
            e.printStackTrace();
        }
    }

    
    private static void exportSimSheet(
        Map<String, Map<String, Stats>> batterStats,
        Map<String, Map<String, Stats>> bowlerStats,
        BaselineCalculator baselineCalculator
) throws Exception {

        CsvWriterUtil csv = new CsvWriterUtil();
        csv.open("sim_stats_fixed.csv"); // new file to compare with old one

        List<String> header = new ArrayList<>();
        header.add("Player");

        // Batting: Player, [role Avg, role SR, role Balls, role BPD, role RPB] per role
        for (String role : ROLE_ORDER) {
            header.add(role + " Avg");
            header.add(role + " SR");
            header.add(role + " Balls");
            header.add(role + " BPD");   // balls per dismissal vs median bowler of this type
            header.add(role + " RPB");   // final RPB vs median bowler of this type
        }

        // Bowling: [hand Balls, hand Avg, hand SR, hand WPB, hand RPB] per hand
        for (String hand : new String[]{"LHB", "RHB"}) {
            header.add(hand + " Balls");
            header.add(hand + " Avg");
            header.add(hand + " SR");
            header.add(hand + " WPB");   // final WPB vs median batter of this hand
            header.add(hand + " RPB");   // final RPB vs median batter of this hand
        }

        csv.writeHeader(header.toArray(new String[0]));

        Set<String> allPlayers = new HashSet<>();
        allPlayers.addAll(batterStats.keySet());
        allPlayers.addAll(bowlerStats.keySet());

        for (String player : allPlayers) {

            List<Object> row = new ArrayList<>();
            row.add(player);

            double sharedWPBBaseline = baselineCalculator.getOverallWicketsPerBall();

            Map<String, Stats> batMap = batterStats.getOrDefault(player, new HashMap<>());
            Map<String, Stats> bowlMap = bowlerStats.getOrDefault(player, new HashMap<>());

            final int MIN_BAT_BALLS   = 100;
            final int MIN_BOWL_BALLS  = 250;
            final double BAT_PENALTY_WPB  = 0.25;  // batter penalty — worst dismissal rate
            final double BOWL_PENALTY_WPB = 0.005; // bowler penalty — worst wicket-taking rate (sort descending)

            // ── Batting columns: Avg, SR, Balls, WPB, RPB per role ────────
            for (String role : ROLE_ORDER) {
                Stats s = batMap.getOrDefault(role, new Stats());

                row.add(s.getBalls() > 0 ? round(s.getBattingAverage()) : "");
                row.add(s.getBalls() > 0 ? round(s.getBattingStrikeRate()) : "");
                row.add(s.getBalls());

                if (s.getBalls() < MIN_BAT_BALLS) {
                    row.add(BAT_PENALTY_WPB);
                    row.add("");
                } else {
                    double batBaselineRPB = baselineCalculator.getBaselineRunsPerBallForRole(role);
                    double batAdjRPB = s.getAdjustedRunsPerBall(batBaselineRPB);
                    double batAdjWPB = s.getAdjustedWicketsPerBall(sharedWPBBaseline);
                    row.add(round((batAdjWPB + sharedWPBBaseline) / 2.0));
                    row.add(round((batAdjRPB + batBaselineRPB) / 2.0));
                }
            }

            // ── Bowling columns: Balls, Avg, SR, WPB, RPB per hand ────────
            for (String hand : new String[]{"LHB", "RHB"}) {
                Stats s = bowlMap.getOrDefault(hand, new Stats());

                row.add(s.getBalls());
                row.add(s.getBalls() > 0 ? round(s.getBowlingAverage()) : "");
                row.add(s.getBalls() > 0 ? round(s.getBowlingStrikeRate()) : "");

                if (s.getBalls() < MIN_BOWL_BALLS) {
                    row.add(BOWL_PENALTY_WPB);
                    row.add("");
                } else {
                    double bowlBaselineRPB = hand.equals("LHB")
                            ? baselineCalculator.getLhbRunsPerBall()
                            : baselineCalculator.getRhbRunsPerBall();
                    double bowlAdjRPB = s.getAdjustedRunsPerBall(bowlBaselineRPB);
                    double bowlAdjWPB = s.getAdjustedWicketsPerBall(sharedWPBBaseline);
                    double medianBatRPB = bowlBaselineRPB;
                    row.add(round((sharedWPBBaseline + bowlAdjWPB) / 2.0));
                    row.add(round((medianBatRPB + bowlAdjRPB) / 2.0));
                }
            }

            csv.writeRow(row.toArray());
        }

        csv.close();
        System.out.println("sim_stats_nerfed.csv generated (with 1000-ball confidence nerf)");
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static StatsBundle buildStats() throws Exception {

        PlayerRoleLoader roleLoader = new PlayerRoleLoader();
        roleLoader.load(com.cricket.engine.PathResolver.resolve("playerRoles.csv"));

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Map<String, Stats>> batterStats = new HashMap<>();
        Map<String, Map<String, Stats>> bowlerStats = new HashMap<>();

        // Aggregate stats from JSON
        Files.list(com.cricket.engine.PathResolver.resolvePath("matches"))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> processMatch(
                        path.toFile(),
                        mapper,
                        roleLoader,
                        batterStats,
                        bowlerStats
                ));

        BaselineCalculator baselineCalculator = new BaselineCalculator();
        baselineCalculator.compute(batterStats, bowlerStats);

        return new StatsBundle(batterStats, bowlerStats, baselineCalculator, roleLoader);
    }
}