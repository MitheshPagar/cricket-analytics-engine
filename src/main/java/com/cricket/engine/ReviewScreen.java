package com.cricket.engine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Map;

public class ReviewScreen {

    private final MatchConfig config;
    private final Runnable onRun;
    private final Runnable onBack;

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

        Button runBtn = new Button("▶  RUN MATCH");
        runBtn.setStyle("-fx-background-color: #d4a030; -fx-text-fill: #0f1923; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 10 32 10 32;");
        runBtn.setOnAction(e -> onRun.run());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().addAll(backBtn, sp, runBtn);

        root.setTop(header);
        root.setCenter(centre);
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
}