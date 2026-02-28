package com.cricket.engine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Screen shown before each team's bowling plan.
 * Lets the user load a saved team or create a new one.
 */
public class TeamSetupScreen {

    private final String teamSlot;       // "Team A" or "Team B"
    private final String stepLabel;      // e.g. "Step 2 of 8"
    private final SavedTeamsStore store;
    private final TeamDatabase db;

    /** Called with (teamName, playerList) when user confirms. */
    private final BiConsumer<String, List<String>> onConfirm;
    private final Runnable onBack;

    public TeamSetupScreen(String teamSlot,
                           String stepLabel,
                           SavedTeamsStore store,
                           TeamDatabase db,
                           BiConsumer<String, List<String>> onConfirm,
                           Runnable onBack) {
        this.teamSlot  = teamSlot;
        this.stepLabel = stepLabel;
        this.store     = store;
        this.db        = db;
        this.onConfirm = onConfirm;
        this.onBack    = onBack;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1923;");

        root.setTop(buildHeader(stage));
        root.setCenter(buildCentre(stage));
        root.setBottom(buildFooter());

        stage.setScene(new Scene(root, 1100, 680));
        stage.show();
    }

    // ── Header ────────────────────────────────────────────────────────────
    private HBox buildHeader(Stage stage) {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #0a1218; -fx-border-color: #d4a030; -fx-border-width: 0 0 2 0;");
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(teamSlot.toUpperCase() + " SETUP");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 20px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label step = new Label(stepLabel);
        step.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #6a8099;");

        header.getChildren().addAll(title, spacer, step);
        return header;
    }

    // ── Centre ────────────────────────────────────────────────────────────
    private VBox buildCentre(Stage stage) {
        VBox centre = new VBox(32);
        centre.setPadding(new Insets(50, 100, 40, 100));
        centre.setAlignment(Pos.TOP_CENTER);

        Label prompt = new Label("How would you like to set up " + teamSlot + "?");
        prompt.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 15px; -fx-text-fill: #c8d8e8;");

        // ── Two option cards ─────────────────────────────────────────────
        HBox cards = new HBox(32);
        cards.setAlignment(Pos.CENTER);

        // New team card
        VBox newCard = buildOptionCard(
                "NEW TEAM",
                "Build a new XI by selecting\nplayers from the database.\nYou can save it for later.",
                "#d4a030",
                () -> showNewTeamBuilder(stage)
        );

        // Load team card
        List<String> savedNames = store.getSavedTeamNames();
        VBox loadCard;

        if (savedNames.isEmpty()) {
            loadCard = buildDisabledCard(
                    "LOAD TEAM",
                    "No saved teams yet.\nCreate a new team first."
            );
        } else {
            loadCard = buildLoadCard(savedNames, stage);
        }

        cards.getChildren().addAll(newCard, loadCard);
        centre.getChildren().addAll(prompt, cards);
        return centre;
    }

    private VBox buildOptionCard(String title, String description,
                                  String accentColor, Runnable onClick) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setPrefSize(280, 200);
        card.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; "
                + "-fx-border-width: 1; -fx-cursor: hand;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; "
                + "-fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        Label descLbl = new Label(description);
        descLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-text-fill: #6a8099; -fx-text-alignment: center;");
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleLbl, descLbl);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #1a3050; -fx-border-color: " + accentColor
                + "; -fx-border-width: 2; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #162030; -fx-border-color: #2a3f55; "
                + "-fx-border-width: 1; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> onClick.run());

        return card;
    }

    private VBox buildDisabledCard(String title, String description) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setPrefSize(280, 200);
        card.setStyle("-fx-background-color: #111820; -fx-border-color: #1a2535; -fx-border-width: 1;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; "
                + "-fx-font-weight: bold; -fx-text-fill: #2a3f55;");

        Label descLbl = new Label(description);
        descLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-text-fill: #2a3f55; -fx-text-alignment: center;");
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleLbl, descLbl);
        return card;
    }

    private VBox buildLoadCard(List<String> savedNames, Stage stage) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setPrefSize(280, 200);
        card.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; -fx-border-width: 1;");

        Label titleLbl = new Label("LOAD TEAM");
        titleLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; "
                + "-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        ComboBox<String> dropdown = new ComboBox<>();
        dropdown.getItems().addAll(savedNames);
        dropdown.setPromptText("Select saved team...");
        dropdown.setPrefWidth(220);
        dropdown.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                + "-fx-background-color: #1e2d3e; -fx-text-fill: #c8d8e8;");

        Button loadBtn = new Button("Load →");
        loadBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 6 16 6 16;");
        loadBtn.setOnAction(e -> {
            String selected = dropdown.getValue();
            if (selected == null) return;
            List<String> players = store.loadTeam(selected);
            if (!players.isEmpty()) {
                onConfirm.accept(selected, players);
            }
        });

        card.getChildren().addAll(titleLbl, dropdown, loadBtn);
        return card;
    }

    // ── Footer ────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: #0a1218; -fx-border-color: #2a3f55; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(14, 24, 14, 24));

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6a8099; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-font-family: 'Courier New'; -fx-padding: 8 20 8 20;");
        backBtn.setOnAction(e -> onBack.run());

        footer.getChildren().add(backBtn);
        return footer;
    }

    // ── Navigate to team builder ──────────────────────────────────────────
    private void showNewTeamBuilder(Stage stage) {
        new TeamBuilderScreen(teamSlot, db, store,
                onConfirm,
                () -> show(stage)
        ).show(stage);
    }
}