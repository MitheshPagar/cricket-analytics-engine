package com.cricket;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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

        System.out.println("🚀 Starting Cricket Stats Engine...");

        
        PlayerRoleLoader roleLoader = new PlayerRoleLoader();
        roleLoader.load("playerRoles.csv");
        System.out.println("✅ Player roles loaded");

        ObjectMapper mapper = new ObjectMapper();

        
        Map<String, Map<String, Stats>> batterStats = new HashMap<>();
        Map<String, Map<String, Stats>> bowlerStats = new HashMap<>();

        
        Files.list(Path.of("matches"))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> processMatch(
                        path.toFile(),
                        mapper,
                        roleLoader,
                        batterStats,
                        bowlerStats
                ));

        System.out.println("📊 Aggregation complete");
        System.out.println("Total batters tracked: " + batterStats.size());
        System.out.println("Total bowlers tracked: " + bowlerStats.size());

        
        System.out.println("🧠 Computing baselines...");
        BaselineCalculator baselineCalculator = new BaselineCalculator();
        baselineCalculator.compute(batterStats, bowlerStats);
        System.out.println("✅ Baselines ready for simulation");

        
        exportSimSheet(batterStats, bowlerStats, baselineCalculator);

        System.out.println("🏁 Pipeline completed successfully");
    }

    
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
            System.err.println("❌ Error processing match: " + file.getName());
            e.printStackTrace();
        }
    }

    
    private static void exportSimSheet(
        Map<String, Map<String, Stats>> batterStats,
        Map<String, Map<String, Stats>> bowlerStats,
        BaselineCalculator baselineCalculator
) throws Exception {

        CsvWriterUtil csv = new CsvWriterUtil();
        csv.open("sim_stats_nerfed.csv"); // new file to compare with old one

        List<String> header = new ArrayList<>();
        header.add("Player");

        for (String role : ROLE_ORDER) {
            header.add(role + " Avg");
            header.add(role + " SR");
            header.add(role + " Adj_RPB");   // Nerfed Runs Per Ball
            header.add(role + " Adj_WPB");   // Nerfed Wickets Per Ball
            header.add(role + " Balls");     // Keep sample size for context
        }

        header.add("LHB Adj_RPB_Conceded");
        header.add("LHB Adj_WPB");
        header.add("LHB Balls");

        header.add("RHB Adj_RPB_Conceded");
        header.add("RHB Adj_WPB");
        header.add("RHB Balls");

        csv.writeHeader(header.toArray(new String[0]));

        Set<String> allPlayers = new HashSet<>();
        allPlayers.addAll(batterStats.keySet());
        allPlayers.addAll(bowlerStats.keySet());

        for (String player : allPlayers) {

            List<Object> row = new ArrayList<>();
            row.add(player);

            // -------------------------
            // BATTING SECTION (NERFED)
            // -------------------------
            Map<String, Stats> batMap = batterStats.getOrDefault(player, new HashMap<>());

            for (String role : ROLE_ORDER) {
                Stats s = batMap.getOrDefault(role, new Stats());

                double baselineRPB = baselineCalculator.getBaselineRunsPerBallForRole(role);
                double baselineWPB = baselineCalculator.getBaselineWicketsPerBallForRole(role);

                double adjRPB = s.getAdjustedRunsPerBall(baselineRPB);
                double adjWPB = s.getAdjustedWicketsPerBall(baselineWPB);

                row.add(round(s.getBattingAverage()));
                row.add(round(s.getBattingStrikeRate()));
                row.add(round(adjRPB));   // 🔥 Nerfed stat
                row.add(round(adjWPB));   // 🔥 Nerfed stat
                row.add(s.getBalls());    // sample size visibility
            }

            Map<String, Stats> bowlMap = bowlerStats.getOrDefault(player, new HashMap<>());

            Stats vsLHB = bowlMap.getOrDefault("LHB", new Stats());
            double lhbBaselineRPB = baselineCalculator.getLhbRunsPerBall();
            double lhbBaselineWPB = baselineCalculator.getLhbWicketsPerBall();

            row.add(round(vsLHB.getAdjustedRunsPerBall(lhbBaselineRPB)));
            row.add(round(vsLHB.getAdjustedWicketsPerBall(lhbBaselineWPB)));
            row.add(vsLHB.getBalls());

            Stats vsRHB = bowlMap.getOrDefault("RHB", new Stats());
            double rhbBaselineRPB = baselineCalculator.getRhbRunsPerBall();
            double rhbBaselineWPB = baselineCalculator.getRhbWicketsPerBall();

            row.add(round(vsRHB.getAdjustedRunsPerBall(rhbBaselineRPB)));
            row.add(round(vsRHB.getAdjustedWicketsPerBall(rhbBaselineWPB)));
            row.add(vsRHB.getBalls());

            csv.writeRow(row.toArray());
        }

        csv.close();
        System.out.println("✅ sim_stats_nerfed.csv generated (with 500-ball confidence nerf)");
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0; // more precision for modeling
    }
}