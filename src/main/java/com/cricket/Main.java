package com.cricket;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    // 🔥 Fixed SIM role order
    private static final String[] ROLE_ORDER = {
            "RF", "LF",
            "RFM", "LFM",
            "RMF", "LMF",
            "RM", "LM",
            "ROS", "LOS",
            "RLS", "LLS"
    };

    public static void main(String[] args) throws Exception {

        System.out.println("🚀 Starting player validation...");

        // 1️⃣ Load roles
        PlayerRoleLoader roleLoader = new PlayerRoleLoader();
        roleLoader.load("playerRoles.csv");
        System.out.println("✅ Roles loaded");

        // 2️⃣ Extract players from JSON
        ObjectMapper mapper = new ObjectMapper();
        Set<String> allPlayers = new HashSet<>();

        Files.list(Path.of("matches"))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> extractPlayers(path.toFile(), mapper, allPlayers));

        System.out.println("📋 Total players found in JSON: " + allPlayers.size());

        // 3️⃣ Validate roles
        int missingCount = 0;
        for (String player : allPlayers) {
            if (!roleLoader.contains(player)) {
                System.out.println("⚠ Missing role for: " + player);
                missingCount++;
            }
        }

        if (missingCount == 0) {
            System.out.println("🎉 All players have assigned roles!");
        } else {
            System.out.println("❌ Missing roles for " + missingCount + " players.");
        }

        // 4️⃣ Stats Engine
        System.out.println("🚀 Starting stats engine...");

        Map<String, Map<String, Stats>> batterStats = new HashMap<>();
        Map<String, Map<String, Stats>> bowlerStats = new HashMap<>();

        Files.list(Path.of("matches"))
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> processMatch(path.toFile(), mapper, roleLoader, batterStats, bowlerStats));

        System.out.println("✅ Stats aggregation complete");
        System.out.println("Total batters tracked: " + batterStats.size());

        // 5️⃣ Export SIM Sheet
        exportSimSheet(batterStats, bowlerStats);
    }

    // ---------------------------------------------------------
    // Extract players
    // ---------------------------------------------------------
    private static void extractPlayers(
            File file,
            ObjectMapper mapper,
            Set<String> allPlayers
    ) {
        try {
            JsonNode root = mapper.readTree(file);
            JsonNode playersNode = root.path("info").path("players");

            playersNode.fields().forEachRemaining(teamEntry -> {
                for (JsonNode player : teamEntry.getValue()) {
                    allPlayers.add(player.asText());
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Failed to read " + file.getName());
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------
    // Process match deliveries
    // ---------------------------------------------------------
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

                        if (bowlRole == null || bowlRole.isBlank())
                            continue;

                        batterStats
                                .computeIfAbsent(batter, k -> new HashMap<>())
                                .computeIfAbsent(bowlRole, k -> new Stats());

                        Stats stats = batterStats.get(batter).get(bowlRole);

                        // Do not count wides as legal balls
                        boolean isWide = delivery.has("extras")
                                && delivery.get("extras").has("wides");

                        if (!isWide) {
                            stats.balls++;
                        }

                        // Runs scored by batter
                        int runs = delivery.get("runs").get("batter").asInt();
                        stats.runs += runs;

                        // Check dismissal
                        if (delivery.has("wickets")) {
                            for (JsonNode wicket : delivery.get("wickets")) {
                                String outPlayer = wicket.get("player_out").asText();
                                if (outPlayer.equals(batter)) {
                                    stats.outs++;
                                }
                            }
                        }

                        // --------------------------------------------------
                        // BOWLER STATS
                        // --------------------------------------------------

                        String batterHand = roleLoader.getBatRole(batter);

                        if (batterHand != null && !batterHand.isBlank()) {

                            bowlerStats
                                .computeIfAbsent(bowler, k -> new HashMap<>())
                                .computeIfAbsent(batterHand, k -> new Stats());

                            Stats bowlStats = bowlerStats.get(bowler).get(batterHand);

                            if (!isWide) {
                            bowlStats.balls++;
                            }

                            int totalRuns = delivery.get("runs").get("total").asInt();
                            bowlStats.runs += totalRuns;

                            if (delivery.has("wickets")) {
                                for (JsonNode wicket : delivery.get("wickets")) {
                                    String outPlayer = wicket.get("player_out").asText();
                                    if (outPlayer.equals(batter)) {
                                        bowlStats.outs++;  // wickets
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing " + file.getName());
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------
    // Export wide SIM sheet
    // ---------------------------------------------------------
    private static void exportSimSheet(
            Map<String, Map<String, Stats>> batterStats,
            Map<String, Map<String, Stats>> bowlerStats
    ) throws Exception {

        CsvWriterUtil csv = new CsvWriterUtil();
        csv.open("player_stats.csv");

        String[] header = buildHeader();
        csv.writeHeader(header);

        for (String player : batterStats.keySet()) {

            String[] row = new String[header.length];
            row[0] = player;

            int colIndex = 1;

    // -------------------------
    // Batting Section
    // -------------------------
            Map<String, Stats> batMap = batterStats.get(player);

            for (String role : ROLE_ORDER) {
                Stats stats = batMap.getOrDefault(role, new Stats());
                row[colIndex++] = String.valueOf(stats.balls);
                row[colIndex++] = String.valueOf(stats.runs);
                row[colIndex++] = String.valueOf(stats.outs);
            }

    // -------------------------
    // Bowling Section
    // -------------------------
            Map<String, Stats> bowlMap = bowlerStats.getOrDefault(player, new HashMap<>());

            Stats vsLHB = bowlMap.getOrDefault("LHB", new Stats());
            row[colIndex++] = String.valueOf(vsLHB.balls);
            row[colIndex++] = String.valueOf(vsLHB.runs);
            row[colIndex++] = String.valueOf(vsLHB.outs);

            Stats vsRHB = bowlMap.getOrDefault("RHB", new Stats());
            row[colIndex++] = String.valueOf(vsRHB.balls);
            row[colIndex++] = String.valueOf(vsRHB.runs);
            row[colIndex++] = String.valueOf(vsRHB.outs);

            csv.writeRow((Object[]) row);
        }

        csv.close();
        System.out.println("✅ player_stats.csv generated");
    }

    // ---------------------------------------------------------
    // Build header
    // ---------------------------------------------------------
    private static String[] buildHeader() {

        int batColumns = ROLE_ORDER.length * 3;
        int bowlColumns = 2 * 3; // LHB + RHB (Balls, Runs, Wkts)

        int totalColumns = 1 + batColumns + bowlColumns;
        String[] header = new String[totalColumns];

        header[0] = "Player";

        int index = 1;

    // Batting columns
        for (String role : ROLE_ORDER) {
            header[index++] = role + " Balls";
            header[index++] = role + " Runs";
            header[index++] = role + " Outs";
        }

    // Bowling columns
        header[index++] = "LHB Balls";
        header[index++] = "LHB Runs";
        header[index++] = "LHB Wkts";

        header[index++] = "RHB Balls";
        header[index++] = "RHB Runs";
        header[index++] = "RHB Wkts";

        return header;
    }
}