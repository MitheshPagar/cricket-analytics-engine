package com.cricket.engine;

import javafx.application.Application;
import javafx.stage.Stage;

public class BowlingAllocatorApp extends Application {

    private static final String PLAYER_ROLES_CSV = "playerRoles.csv";

    private Stage primaryStage;
    private MatchConfig config;
    private TeamDatabase db;
    private SavedTeamsStore store;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.config = new MatchConfig();
        this.store  = new SavedTeamsStore();

        // Load player database once
        this.db = new TeamDatabase();
        db.load(PLAYER_ROLES_CSV);

        primaryStage.setTitle("Test Match Planner");
        primaryStage.setResizable(true);

        showPitchSetup();
    }

    // ── Screen 1: Pitch Setup ─────────────────────────────────────────────
    private void showPitchSetup() {
        new PitchSetupScreen(config, () -> showTeamSetup(true))
                .show(primaryStage);
    }

    // ── Screens 2-3 / 5-6: Team Setup (Load or New) ──────────────────────
    private void showTeamSetup(boolean isTeamA) {
        String slot = isTeamA ? "Team A" : "Team B";
        String step = isTeamA ? "Step 2 of 8" : "Step 5 of 8";

        new TeamSetupScreen(slot, step, store, db,
                (teamName, players) -> {
                    if (isTeamA) {
                        config.teamAName = teamName;
                        config.teamAXI   = players;
                        showBowlingAllocation(true);
                    } else {
                        config.teamBName = teamName;
                        config.teamBXI   = players;
                        showBowlingAllocation(false);
                    }
                },
                () -> {
                    if (isTeamA) showPitchSetup();
                    else showBowlingAllocation(true);
                }
        ).show(primaryStage);
    }

    // ── Screens 4 & 7: Bowling Allocation ────────────────────────────────
    private void showBowlingAllocation(boolean isTeamA) {
        // Team A bowls at Team B's XI, Team B bowls at Team A's XI
        String title  = isTeamA
                ? config.teamAName + "'s Bowling Plan  (bowling at " + config.teamBName + ")"
                : config.teamBName + "'s Bowling Plan  (bowling at " + config.teamAName + ")";
        java.util.List<String> bowlingAtXI = isTeamA ? config.teamBXI : config.teamAXI;

        // Build BowlerInfo list using roles from TeamDatabase
        java.util.List<BowlerInfo> bowlerInfos = new java.util.ArrayList<>();
        java.util.List<String> bowlingXI = isTeamA ? config.teamAXI : config.teamBXI;
        for (String name : bowlingXI) {
            PlayerRecord rec = db.findByName(name);
            String role = (rec != null) ? rec.getBowlRole() : "";
            bowlerInfos.add(new BowlerInfo(name, role));
        }

        BowlingAllocationScreen allocScreen = new BowlingAllocationScreen(title, bowlingAtXI, config,
                plan -> {
                    if (isTeamA) {
                        config.teamABowlingPlan = plan;
                        showTeamSetup(false);
                    } else {
                        config.teamBBowlingPlan = plan;
                        showReview();
                    }
                },
                () -> {
                    if (isTeamA) showTeamSetup(true);
                    else showTeamSetup(false);
                }
        );
        allocScreen.setBowlerInfoList(bowlerInfos);
        allocScreen.show(primaryStage);
    }

    // ── Screen 8: Review & Run ────────────────────────────────────────────
    private void showReview() {
        new ReviewScreen(config,
                () -> {
                    primaryStage.close();
                    new Thread(() -> MatchLauncher.launch(config)).start();
                },
                () -> showBowlingAllocation(false)
        ).show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}