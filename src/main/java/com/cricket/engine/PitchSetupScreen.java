package com.cricket.engine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class PitchSetupScreen {

    private final MatchConfig config;
    private final Runnable onNext;

    // Slider values
    private double green    = 1.0;
    private double dry      = 1.0;
    private double bounce   = 1.0;
    private double flat     = 1.0;
    private double boundary = 1.0;

    public PitchSetupScreen(MatchConfig config, Runnable onNext) {
        this.config = config;
        this.onNext = onNext;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1923;");

        // ── Header ──────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #0a1218; -fx-border-color: #d4a030; -fx-border-width: 0 0 2 0;");
        header.setPadding(new Insets(16, 24, 16, 24));

        Label title = new Label("PITCH CONDITIONS");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 20px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");

        Label step = new Label("Step 1 of 4");
        step.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #6a8099;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, step);

        // ── Centre: sliders ──────────────────────────────────────────────────
        VBox centre = new VBox(20);
        centre.setPadding(new Insets(40, 80, 40, 80));
        centre.setAlignment(Pos.CENTER);

        Label subtitle = new Label("Set the pitch conditions for this match");
        subtitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-text-fill: #c8d8e8;");

        centre.getChildren().add(subtitle);

        String[][] conditions = {
            {"Green (seam movement)",    "green",    "1.0"},
            {"Dry (spin / rough)",       "dry",      "1.0"},
            {"Bounce (pace & carry)",    "bounce",   "1.0"},
            {"Flat (batting ease)",      "flat",     "1.0"},
            {"Boundary (outfield speed)","boundary", "1.0"}
        };

        double[] values = {green, dry, bounce, flat, boundary};

        for (int i = 0; i < conditions.length; i++) {
            final int idx = i;
            String label = conditions[i][0];

            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(600);

            Label lbl = new Label(label);
            lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; "
                    + "-fx-text-fill: #c8d8e8;");
            lbl.setPrefWidth(240);

            Slider slider = new Slider(0.5, 2.0, 1.0);
            slider.setPrefWidth(240);
            slider.setStyle("-fx-accent: #d4a030;");

            Label valLbl = new Label("1.0");
            valLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; "
                    + "-fx-text-fill: #d4a030; -fx-font-weight: bold;");
            valLbl.setPrefWidth(40);

            slider.valueProperty().addListener((obs, o, n) -> {
                values[idx] = n.doubleValue();
                valLbl.setText(String.format("%.1f", n.doubleValue()));
                green    = values[0];
                dry      = values[1];
                bounce   = values[2];
                flat     = values[3];
                boundary = values[4];
            });

            row.getChildren().addAll(lbl, slider, valLbl);
            centre.getChildren().add(row);
        }

        // ── Footer: Next button ──────────────────────────────────────────────
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: #0a1218; -fx-border-color: #2a3f55; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(14, 24, 14, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button nextBtn = new Button("Next: India's Bowling Plan →");
        nextBtn.setStyle("-fx-background-color: #d4a030; -fx-text-fill: #0f1923; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 20 8 20;");
        nextBtn.setOnAction(e -> {
            config.pitchProfile = new PitchProfile(green, dry, bounce, flat, boundary);
            onNext.run();
        });

        footer.getChildren().add(nextBtn);

        root.setTop(header);
        root.setCenter(centre);
        root.setBottom(footer);

        stage.setScene(new Scene(root, 1100, 680));
        stage.show();
    }
}