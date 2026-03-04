package com.cricket.engine;

import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ReviewScreen {

    private final MatchConfig config;
    private final Runnable onRun;
    private final Runnable onBack;
    private VBox simResultBox;
    private javafx.scene.control.TextField simCountField;

    public ReviewScreen(MatchConfig config, Runnable onRun, Runnable onBack) {
        this.config = config;
        this.onRun  = onRun;
        this.onBack = onBack;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1923;");

        // ── Header ──────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #0a1218; -fx-border-color: #d4a030; -fx-border-width: 0 0 2 0;");
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("MATCH REVIEW");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 20px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");

        Label step = new Label("Step 4 of 4");
        step.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #6a8099;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, step);

        // ── Centre: two-column summary ───────────────────────────────────────
        HBox centre = new HBox(40);
        centre.setPadding(new Insets(40, 60, 40, 60));
        centre.setAlignment(Pos.TOP_CENTER);

        centre.getChildren().addAll(
                buildPitchSummary(),
                buildTeamBowlingSummary(config.teamAName + " bowling",
                        config.teamAXI, config.teamABowlingPlan),
                buildTeamBowlingSummary(config.teamBName + " bowling",
                        config.teamBXI, config.teamBBowlingPlan)
        );

        // ── Footer ───────────────────────────────────────────────────────────
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: #0a1218; -fx-border-color: #2a3f55; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(16, 24, 16, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setSpacing(12);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6a8099; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-font-family: 'Courier New'; -fx-padding: 8 20 8 20;");
        backBtn.setOnAction(e -> onBack.run());

        simCountField = new javafx.scene.control.TextField("500");
        simCountField.setPrefWidth(80);
        simCountField.setStyle("-fx-background-color: #1e2d3e; -fx-text-fill: #c8d8e8; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; "
                + "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px; "
                + "-fx-padding: 8 8 8 8;");

        Label simLabel = new Label("×  sims");
        simLabel.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px; -fx-text-fill: #6a8099;");

        Button simBtn = new Button("⚡ SIMULATE");
        simBtn.setStyle("-fx-background-color: #1a3050; -fx-text-fill: #d4a030; "
                + "-fx-border-color: #d4a030; -fx-border-width: 1; "
                + "-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 10 24 10 24;");
        simBtn.setOnAction(e -> runSimulation(simBtn));

        Button runBtn = new Button("▶  RUN MATCH");
        runBtn.setStyle("-fx-background-color: #d4a030; -fx-text-fill: #0f1923; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 10 32 10 32;");
        runBtn.setOnAction(e -> onRun.run());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().addAll(backBtn, sp, simCountField, simLabel, simBtn, runBtn);

        simResultBox = new VBox();
        simResultBox.setStyle("-fx-background-color: #0f1923;");

        VBox mainContent = new VBox();
        mainContent.getChildren().addAll(centre, simResultBox);
        ScrollPane scroll = new ScrollPane(mainContent);
        scroll.setStyle("-fx-background-color: #0f1923; -fx-background: #0f1923;");
        scroll.setFitToWidth(true);

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);

        stage.setScene(new Scene(root, 1100, 680));
        stage.show();
    }

    // ── Pitch summary card ────────────────────────────────────────────────
    private VBox buildPitchSummary() {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; "
                + "-fx-border-width: 1; -fx-padding: 20;");
        card.setPrefWidth(240);

        Label title = new Label("PITCH");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");
        card.getChildren().add(title);
        card.getChildren().add(new Separator());

        if (config.pitchProfile != null) {
            PitchProfile p = config.pitchProfile;
            addPitchRow(card, "Green",    p.getGreen());
            addPitchRow(card, "Dry",      p.getDry());
            addPitchRow(card, "Bounce",   p.getBounce());
            addPitchRow(card, "Flat",     p.getFlat());
            addPitchRow(card, "Boundary", p.getBoundary());
        } else {
            Label none = new Label("Default (neutral)");
            none.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #6a8099;");
            card.getChildren().add(none);
        }

        return card;
    }

    private void addPitchRow(VBox card, String label, double value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #c8d8e8;");
        lbl.setPrefWidth(80);

        String color = value > 1.2 ? "#d4a030" : value < 0.9 ? "#c0392b" : "#27ae60";
        Label val = new Label(String.format("%.1f", value));
        val.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        row.getChildren().addAll(lbl, val);
        card.getChildren().add(row);
    }

    // ── Team bowling summary card ─────────────────────────────────────────
    private VBox buildTeamBowlingSummary(String heading,
                                          java.util.List<String> xi,
                                          BowlingPlan plan) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; "
                + "-fx-border-width: 1; -fx-padding: 20;");
        card.setPrefWidth(280);

        Label title = new Label(heading.toUpperCase());
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");
        card.getChildren().add(title);
        card.getChildren().add(new Separator());

        if (plan == null) {
            Label none = new Label("No plan set — will use full XI rotation");
            none.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #6a8099; -fx-font-style: italic;");
            none.setWrapText(true);
            card.getChildren().add(none);
            return card;
        }

        Map<String, Integer> counts = plan.getOverCounts();
        int total = 0;

        for (String player : xi) {
            int count = counts.getOrDefault(player, 0);
            if (count == 0) continue;
            total += count;

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLbl = new Label(player);
            nameLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #c8d8e8;");
            nameLbl.setPrefWidth(180);

            String color = count >= 20 ? "#d4a030" : count >= 10 ? "#27ae60" : "#6a8099";
            Label countLbl = new Label(count + " ov");
            countLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                    + "-fx-text-fill: " + color + "; -fx-font-weight: bold;");

            row.getChildren().addAll(nameLbl, countLbl);
            card.getChildren().add(row);
        }

        card.getChildren().add(new Separator());

        int unassigned = 90 - total;
        Label totalLbl = new Label("Assigned: " + total + " / 90 overs");
        totalLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                + "-fx-text-fill: #c8d8e8; -fx-font-weight: bold;");
        card.getChildren().add(totalLbl);

        if (unassigned > 0) {
            Label warn = new Label("⚠  " + unassigned + " overs unassigned (will cycle last bowler)");
            warn.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                    + "-fx-text-fill: #d4a030; -fx-font-style: italic;");
            warn.setWrapText(true);
            card.getChildren().add(warn);
        }

        return card;
    }

    // ── Monte Carlo Simulation ─────────────────────────────────────────────
    private void runSimulation(javafx.scene.control.Button simBtn) {
        int simCount;
        try {
            simCount = Integer.parseInt(simCountField.getText().trim());
            if (simCount < 1 || simCount > 10000) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            simCountField.setStyle(simCountField.getStyle() + "-fx-border-color: #c0392b;");
            return;
        }

        simBtn.setDisable(true);
        simBtn.setText("Simulating...");
        simResultBox.getChildren().clear();

        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(400);
        bar.setStyle("-fx-accent: #d4a030;");
        Label progressLbl = new Label("Running " + simCount + " simulations...");
        progressLbl.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px; -fx-text-fill: #6a8099;");

        VBox loadingBox = new VBox(8, progressLbl, bar);
        loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
        loadingBox.setPadding(new Insets(20));
        simResultBox.getChildren().add(loadingBox);

        final int finalSimCount = simCount;
        new Thread(() -> {
            MonteCarloEngine.SimResult result = MonteCarloEngine.run(config, finalSimCount, progress ->
                    Platform.runLater(() -> bar.setProgress(progress / (double) finalSimCount))
            );

            // Export to Excel
            try {
                String path = System.getProperty("user.home") + "/allStats.xlsx";
                StatsExporter.export(result, config.teamAName, config.teamBName, finalSimCount, path);
                System.out.println("Stats exported to: " + path);
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }

            Platform.runLater(() -> {
                simResultBox.getChildren().clear();
                simResultBox.getChildren().add(buildSimResults(result, finalSimCount));
                simBtn.setDisable(false);
                simBtn.setText("⚡ SIMULATE");
            });
        }).start();
    }

    private VBox buildSimResults(MonteCarloEngine.SimResult r, int simCount) {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20, 60, 20, 60));
        box.setStyle("-fx-background-color: #0a1218; -fx-border-color: #d4a030; -fx-border-width: 1 0 0 0;");

        Label heading = new Label("SIMULATION RESULTS  (" + simCount + " matches)");
        heading.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 13px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");
        box.getChildren().add(heading);

        // Win/Draw counts
        HBox bars = new HBox(40);
        bars.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bars.getChildren().addAll(
                buildWinBlock(config.teamAName, r.teamAWins, simCount, "#c0392b"),
                buildWinBlock("Draw",            r.draws,    simCount, "#4a5568"),
                buildWinBlock(config.teamBName, r.teamBWins, simCount, "#27ae60")
        );
        box.getChildren().add(bars);

        // Top performers
        HBox performers = new HBox(60);
        performers.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox runBox = new VBox(4);
        Label runTitle = new Label("🏏  TOP RUN SCORER");
        runTitle.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10px; -fx-text-fill: #6a8099; -fx-font-weight: bold;");
        Label runVal = new Label(r.topRunScorer());
        runVal.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px; -fx-text-fill: #c8d8e8;");
        runBox.getChildren().addAll(runTitle, runVal);

        VBox wktBox = new VBox(4);
        Label wktTitle = new Label("🎯  TOP WICKET TAKER");
        wktTitle.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10px; -fx-text-fill: #6a8099; -fx-font-weight: bold;");
        Label wktVal = new Label(r.topWicketTaker());
        wktVal.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px; -fx-text-fill: #c8d8e8;");
        wktBox.getChildren().addAll(wktTitle, wktVal);

        performers.getChildren().addAll(runBox, wktBox);
        box.getChildren().add(performers);

        return box;
    }

    private VBox buildWinBlock(String label, int wins, int total, String color) {
        VBox block = new VBox(6);
        block.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        block.setPrefWidth(220);

        Label nameLbl = new Label(label.toUpperCase());
        nameLbl.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 11px; "
                + "-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        Label winsLbl = new Label(wins + " / " + total);
        winsLbl.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-size: 22px; "
                + "-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        double pct = total == 0 ? 0 : wins / (double) total;
        ProgressBar bar = new ProgressBar(pct);
        bar.setPrefWidth(200);
        bar.setStyle("-fx-accent: " + color + ";");

        block.getChildren().addAll(nameLbl, winsLbl, bar);
        return block;
    }
}