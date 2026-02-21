package com.cricket.engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
        Files.list(Path.of("matches"))
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
        // Create BallEngine
        // ----------------------------
        BallEngine ballEngine = new BallEngine(
                batterStats,
                bowlerStats,
                baselineCalculator
        );

        // Pick random batter + bowler
        String testBatter = batterStats.keySet().iterator().next();
        String testBowler = bowlerStats.keySet().iterator().next();

        System.out.println("Testing Batter: " + testBatter);
        System.out.println("Testing Bowler: " + testBowler);

        // ----------------------------
        // Simulate 10,000 balls
        // ----------------------------
        int balls = 10000;
        int runs = 0;
        int wickets = 0;

        Map<BallOutcome, Integer> distribution = new HashMap<>();

        for (int i = 0; i < balls; i++) {

            BallOutcome outcome = ballEngine.simulateBall(
                    testBatter,
                    testBowler,
                    "RF",     // use a bowl role you know exists
                    "RHB"     // use hand type
            );

            distribution.merge(outcome, 1, Integer::sum);

            if (outcome.isWicket()) {
                wickets++;
            } else {
                runs += outcome.getRuns();
            }
        }

        System.out.println("Balls: " + balls);
        System.out.println("Runs: " + runs);
        System.out.println("Wickets: " + wickets);
        System.out.println("Runs Per Ball: " + ((double) runs / balls));
        System.out.println("Wicket Rate: " + ((double) wickets / balls));
        System.out.println("Distribution: " + distribution);

        System.out.println("🏁 BallEngine test complete");
    }

    // ----------------------------
    // JSON Processing (Same as Main)
    // ----------------------------
    @SuppressWarnings({"UseSpecificCatch", "CallToPrintStackTrace"})
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

                        // Batting stats
                        if (bowlRole != null) {
                            batterStats
                                    .computeIfAbsent(batter, k -> new HashMap<>())
                                    .computeIfAbsent(bowlRole, k -> new Stats())
                                    .recordBall(batterRuns, isWicket);
                        }

                        // Bowling stats
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