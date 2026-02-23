package com.cricket.engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cricket.BaselineCalculator;
import com.cricket.PlayerRoleLoader;
import com.cricket.Stats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BallEngineTest {

    public static void main(String[] args) throws Exception {

        System.out.println("🚀 Building stats for BallEngine test...");

        PlayerRoleLoader roleLoader = new PlayerRoleLoader();
        roleLoader.load("playerRoles.csv");

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Map<String, Stats>> batterStats = new HashMap<>();
        Map<String, Map<String, Stats>> bowlerStats = new HashMap<>();

        // ----------------------------
        // Build stats from JSON
        // ----------------------------
        Files.list(Paths.get("matches"))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> processMatch(
                        path.toFile(),
                        mapper,
                        roleLoader,
                        batterStats,
                        bowlerStats
                ));

        System.out.println("✅ Aggregation complete");

        // ----------------------------
        // Compute baselines
        // ----------------------------
        BaselineCalculator baselineCalculator = new BaselineCalculator();
        baselineCalculator.compute(batterStats, bowlerStats);

        System.out.println("✅ Baselines ready");

        // ----------------------------
        // Create Engines
        // ----------------------------
        BallEngine ballEngine = new BallEngine(
                batterStats,
                bowlerStats,
                baselineCalculator
        );

        InningsEngine inningsEngine = new InningsEngine(ballEngine);

        // ----------------------------
        // Select XI
        // ----------------------------
        List<String> battingXI = new ArrayList<>(batterStats.keySet()).subList(0, 11);
        List<String> bowlingXI = new ArrayList<>(bowlerStats.keySet()).subList(0, 5);

        System.out.println("Batting XI: " + battingXI);
        System.out.println("Bowling XI: " + bowlingXI);

        // ----------------------------
        // Simulate Innings (90 overs)
        // ----------------------------
        InningsResult result = inningsEngine.simulateInnings(
                battingXI,
                bowlingXI,
                "RF",   // Using RF role for testing
                "RHB",  // Assume right-handed striker for now
                90      // 90 overs = full Test day
        );

        System.out.println("🏏 Test Innings Result: " + result);
        System.out.println("🏁 Innings simulation complete");
    }

    // ----------------------------
    // JSON Aggregation Logic
    // ----------------------------
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
                                if (wicket.get("player_out").asText().equals(batter)) {
                                    isWicket = true;
                                }
                            }
                        }

                        if (bowlRole != null) {
                            batterStats
                                    .computeIfAbsent(batter, k -> new HashMap<>())
                                    .computeIfAbsent(bowlRole, k -> new Stats())
                                    .recordBall(batterRuns, isWicket);
                        }

                        if (batterHand != null) {
                            bowlerStats
                                    .computeIfAbsent(bowler, k -> new HashMap<>())
                                    .computeIfAbsent(batterHand, k -> new Stats())
                                    .recordBall(totalRuns, isWicket);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}