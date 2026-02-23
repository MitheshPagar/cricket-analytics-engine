package com.cricket.engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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

        System.out.println("🚀 Building stats for Team vs Team simulation...");

        PlayerRoleLoader roleLoader = new PlayerRoleLoader();
        roleLoader.load("playerRoles.csv");

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Map<String, Stats>> batterStats = new HashMap<>();
        Map<String, Map<String, Stats>> bowlerStats = new HashMap<>();

        // Aggregate stats from JSON
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

        BaselineCalculator baselineCalculator = new BaselineCalculator();
        baselineCalculator.compute(batterStats, bowlerStats);

        System.out.println("✅ Baselines ready");

        BallEngine ballEngine = new BallEngine(
                batterStats,
                bowlerStats,
                baselineCalculator
        );

        InningsEngine inningsEngine = new InningsEngine(ballEngine, roleLoader);

        // ----------------------------
        // CUSTOM TEAM A (Batting)
        // ----------------------------
        List<String> teamA = Arrays.asList(
                "RG Sharma",
                "Shubman Gill",
                "CA Pujara",
                "V Kohli",
                "AM Rahane",
                "RR Pant",
                "RA Jadeja",
                "R Ashwin",
                "AR Patel",
                "Mohammed Siraj",
                "JJ Bumrah"
        );

        // ----------------------------
        // CUSTOM TEAM B (Bowling)
        // ----------------------------
        List<String> teamB = Arrays.asList(
                "PJ Cummins",
                "MA Starc",
                "DW Steyn",
                "SK Warne",
                "K Rabada"
        );

        System.out.println("Batting XI: " + teamA);
        System.out.println("Bowling Attack: " + teamB);

        // Simulate 90 overs
        InningsResult result = inningsEngine.simulateInnings(
                teamA,
                teamB,
                90
        );

        System.out.println("🏏 Test Innings Result: " + result);
        System.out.println("🏁 Simulation complete");
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