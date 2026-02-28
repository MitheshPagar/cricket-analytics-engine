package com.cricket.engine;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

public class BowlingAllocatorApp extends Application {

    private static final List<String> INDIA_XI = List.of(
            "RG Sharma", "Shubman Gill", "CA Pujara", "V Kohli",
            "AM Rahane", "RR Pant", "RA Jadeja", "R Ashwin",
            "AR Patel", "Mohammed Siraj", "JJ Bumrah"
    );

    private static final List<String> AUSTRALIA_XI = List.of(
            "DA Warner", "UT Khawaja", "M Labuschagne", "SPD Smith",
            "TM Head", "MR Marsh", "AT Carey", "MA Starc",
            "PJ Cummins", "NM Lyon", "JR Hazlewood"
    );

    private Stage primaryStage;
    private MatchConfig config;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.config = new MatchConfig("India", INDIA_XI, "Australia", AUSTRALIA_XI);
        primaryStage.setTitle("Test Match Planner");
        primaryStage.setResizable(true);
        showPitchSetup();
    }

    private void showPitchSetup() {
        new PitchSetupScreen(config, () -> showBowlingAllocation(true))
                .show(primaryStage);
    }

    private void showBowlingAllocation(boolean isTeamA) {
        String title  = isTeamA
                ? "India's Bowling Plan  (bowling at Australia)"
                : "Australia's Bowling Plan  (bowling at India)";
        List<String> xi = isTeamA ? AUSTRALIA_XI : INDIA_XI;

        new BowlingAllocationScreen(title, xi, config,
                plan -> {
                    if (isTeamA) { config.teamABowlingPlan = plan; showBowlingAllocation(false); }
                    else         { config.teamBBowlingPlan = plan; showReview(); }
                },
                () -> { if (isTeamA) showPitchSetup(); else showBowlingAllocation(true); }
        ).show(primaryStage);
    }

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