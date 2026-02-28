package com.cricket.engine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Player picker screen. Shows a searchable player list on the left,
 * and the current XI slots (1–11) on the right.
 * User clicks a player to add them; clicks a slot to remove.
 */
public class TeamBuilderScreen {

    private static final int XI_SIZE = 11;

    private final String teamSlot;
    private final TeamDatabase db;
    private final SavedTeamsStore store;
    private final BiConsumer<String, List<String>> onConfirm;
    private final Runnable onBack;

    // State
    private final List<String> selectedPlayers = new ArrayList<>();
    private String teamName = "";

    // UI refs
    private VBox xiSlotsBox;
    private ListView<String> playerListView;
    private Label statusLabel;
    private Label countLabel;
    private TextField teamNameField;
    private Stage stage;

    public TeamBuilderScreen(String teamSlot,
                             TeamDatabase db,
                             SavedTeamsStore store,
                             BiConsumer<String, List<String>> onConfirm,
                             Runnable onBack) {
        this.teamSlot  = teamSlot;
        this.db        = db;
        this.store     = store;
        this.onConfirm = onConfirm;
        this.onBack    = onBack;
    }

    public void show(Stage stage) {
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1923;");

        root.setTop(buildHeader());
        root.setLeft(buildPlayerPanel());
        root.setRight(buildXIPanel());
        root.setBottom(buildFooter(stage));

        stage.setScene(new Scene(root, 1280, 820));
        stage.show();

        refreshPlayerList("");
        refreshXISlots();
    }

    // ── Header ────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setStyle("-fx-background-color: #0a1218; -fx-border-color: #d4a030; -fx-border-width: 0 0 2 0;");
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("BUILD " + teamSlot.toUpperCase());
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 18px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Click player to add  •  Click slot to remove");
        hint.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #6a8099; -fx-font-style: italic;");

        header.getChildren().addAll(title, spacer, hint);
        return header;
    }

    // ── Left panel: search + player list ─────────────────────────────────
    private VBox buildPlayerPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; -fx-border-width: 0 1 0 0;");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(460);

        Label title = new Label("PLAYER DATABASE  (" + db.size() + " players)");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");

        // Search box
        TextField searchBox = new TextField();
        searchBox.setPromptText("Search by name...");
        searchBox.setStyle("-fx-background-color: #1e2d3e; -fx-text-fill: #c8d8e8; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; "
                + "-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                + "-fx-prompt-text-fill: #3a5570; -fx-padding: 6 10 6 10;");
        searchBox.textProperty().addListener((obs, o, n) -> refreshPlayerList(n));

        // Column headers
        HBox colHeaders = new HBox();
        colHeaders.setPadding(new Insets(4, 8, 4, 8));
        colHeaders.setStyle("-fx-background-color: #0f1923;");
        Label nameHdr = new Label("Player");
        nameHdr.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #6a8099;");
        nameHdr.setPrefWidth(220);
        Label batHdr = new Label("Bat");
        batHdr.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #6a8099;");
        batHdr.setPrefWidth(60);
        Label bowlHdr = new Label("Bowl");
        bowlHdr.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #6a8099;");
        bowlHdr.setPrefWidth(80);
        colHeaders.getChildren().addAll(nameHdr, batHdr, bowlHdr);

        // Player list
        playerListView = new ListView<>();
        playerListView.setStyle("-fx-background-color: #1e2d3e; -fx-border-color: #2a3f55;");
        playerListView.setPrefHeight(580);
        playerListView.setCellFactory(lv -> new PlayerCell());
        playerListView.setOnMouseClicked(e -> {
            String selected = playerListView.getSelectionModel().getSelectedItem();
            if (selected != null) addPlayer(selected);
        });

        panel.getChildren().addAll(title, searchBox, colHeaders, playerListView);
        return panel;
    }

    // ── Right panel: XI slots ─────────────────────────────────────────────
    private VBox buildXIPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; -fx-border-width: 0 0 0 1;");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(380);

        // Team name field
        Label nameTitle = new Label("TEAM NAME");
        nameTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");

        teamNameField = new TextField();
        teamNameField.setPromptText("Enter team name...");
        teamNameField.setStyle("-fx-background-color: #1e2d3e; -fx-text-fill: #c8d8e8; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; "
                + "-fx-font-family: 'Courier New'; -fx-font-size: 13px; "
                + "-fx-prompt-text-fill: #3a5570; -fx-padding: 6 10 6 10;");
        teamNameField.textProperty().addListener((obs, o, n) -> teamName = n.trim());

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a3f55;");

        Label xiTitle = new Label("PLAYING XI");
        xiTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");

        countLabel = new Label("0 / 11 selected");
        countLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #6a8099;");

        xiSlotsBox = new VBox(5);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #d4a030;");
        statusLabel.setWrapText(true);

        panel.getChildren().addAll(nameTitle, teamNameField, sep, xiTitle,
                countLabel, xiSlotsBox, statusLabel);
        return panel;
    }

    // ── Footer ────────────────────────────────────────────────────────────
    private HBox buildFooter(Stage stage) {
        HBox footer = new HBox(12);
        footer.setStyle("-fx-background-color: #0a1218; -fx-border-color: #2a3f55; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(14, 24, 14, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6a8099; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-font-family: 'Courier New'; -fx-padding: 8 20 8 20;");
        backBtn.setOnAction(e -> onBack.run());

        Button clearBtn = new Button("Clear XI");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c0392b; "
                + "-fx-border-color: #c0392b; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-font-family: 'Courier New'; -fx-padding: 8 20 8 20;");
        clearBtn.setOnAction(e -> {
            selectedPlayers.clear();
            refreshXISlots();
            refreshPlayerList(null);
        });

        Button saveBtn = new Button("Save Team");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 20 8 20;");
        saveBtn.setOnAction(e -> saveTeam());

        Button confirmBtn = new Button("Confirm XI →");
        confirmBtn.setStyle("-fx-background-color: #d4a030; -fx-text-fill: #0f1923; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 20 8 20;");
        confirmBtn.setOnAction(e -> confirmXI());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(backBtn, spacer, clearBtn, saveBtn, confirmBtn);
        return footer;
    }

    // ── Logic ─────────────────────────────────────────────────────────────
    private void addPlayer(String name) {
        if (selectedPlayers.contains(name)) {
            setStatus("⚠  " + name + " is already in the XI.");
            return;
        }
        if (selectedPlayers.size() >= XI_SIZE) {
            setStatus("⚠  XI is full. Remove a player first.");
            return;
        }
        selectedPlayers.add(name);
        refreshXISlots();
        refreshPlayerList(null);
        setStatus("Added: " + name);
    }

    private void removePlayer(int index) {
        if (index < 0 || index >= selectedPlayers.size()) return;
        String removed = selectedPlayers.remove(index);
        refreshXISlots();
        refreshPlayerList(null);
        setStatus("Removed: " + removed);
    }

    private void confirmXI() {
        if (teamName.isBlank()) {
            setStatus("⚠  Please enter a team name first.");
            return;
        }
        if (selectedPlayers.size() != XI_SIZE) {
            setStatus("⚠  Select exactly 11 players (" + selectedPlayers.size() + " selected).");
            return;
        }
        onConfirm.accept(teamName, new ArrayList<>(selectedPlayers));
    }

    private void saveTeam() {
        if (teamName.isBlank()) {
            setStatus("⚠  Enter a team name before saving.");
            return;
        }
        if (selectedPlayers.isEmpty()) {
            setStatus("⚠  Select at least one player before saving.");
            return;
        }
        try {
            store.saveTeam(teamName, selectedPlayers);
            setStatus("✓  Team '" + teamName + "' saved (" + selectedPlayers.size() + " players).");
        } catch (Exception ex) {
            setStatus("✗  Save failed: " + ex.getMessage());
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────
    private void refreshPlayerList(String query) {
        if (playerListView == null) return;
        String q = (query == null)
                ? (playerListView.getUserData() instanceof String s ? s : "")
                : query;
        playerListView.setUserData(q);

        List<PlayerRecord> results = db.search(q);
        playerListView.getItems().clear();
        for (PlayerRecord p : results) {
            // Mark already-selected players
            String display = selectedPlayers.contains(p.getName())
                    ? "✓ " + p.getName() + " | " + p.getRoleSummary()
                    :       p.getName() + " | " + p.getRoleSummary();
            playerListView.getItems().add(display);
        }
    }

    private void refreshXISlots() {
        xiSlotsBox.getChildren().clear();

        for (int i = 0; i < XI_SIZE; i++) {
            final int idx = i;
            HBox slot = new HBox(10);
            slot.setAlignment(Pos.CENTER_LEFT);
            slot.setPadding(new Insets(6, 10, 6, 10));

            Label numLbl = new Label((i + 1) + ".");
            numLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                    + "-fx-text-fill: #6a8099; -fx-font-weight: bold;");
            numLbl.setPrefWidth(28);

            if (i < selectedPlayers.size()) {
                String playerName = selectedPlayers.get(i);
                PlayerRecord rec  = db.findByName(playerName);

                String accentColor = rec != null
                        ? accentFor(rec.getBowlerCategory()) : "#6a8099";

                slot.setStyle("-fx-background-color: #1e2d3e; -fx-border-color: "
                        + accentColor + " #2a3f55 #2a3f55 " + accentColor
                        + "; -fx-border-width: 1 1 1 3; -fx-cursor: hand;");

                Label nameLbl = new Label(playerName);
                nameLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #c8d8e8;");
                nameLbl.setPrefWidth(180);

                Label roleLbl = rec != null
                        ? new Label(rec.getRoleSummary()) : new Label("?");
                roleLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                        + "-fx-text-fill: " + accentColor + ";");

                Label removeLbl = new Label("✕");
                removeLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                        + "-fx-text-fill: #c0392b; -fx-cursor: hand;");
                removeLbl.setOnMouseClicked(e -> removePlayer(idx));

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                slot.getChildren().addAll(numLbl, nameLbl, roleLbl, sp, removeLbl);
                slot.setOnMouseClicked(e -> removePlayer(idx));

            } else {
                slot.setStyle("-fx-background-color: #111820; -fx-border-color: #1a2535; -fx-border-width: 1;");
                Label emptyLbl = new Label("— empty slot —");
                emptyLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #2a3f55;");
                slot.getChildren().addAll(numLbl, emptyLbl);
            }

            xiSlotsBox.getChildren().add(slot);
        }

        // Update count label
        int count = selectedPlayers.size();
        String color = count == XI_SIZE ? "#27ae60" : count > 0 ? "#d4a030" : "#6a8099";
        countLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                + "-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        countLabel.setText(count + " / 11 selected");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private String accentFor(BowlerInfo.Category cat) {
        return switch (cat) {
            case FAST        -> "#c0392b";
            case MEDIUM_FAST -> "#e67e22";
            case MEDIUM      -> "#b8860b";
            case SPIN        -> "#27ae60";
            case PART_TIME   -> "#4a5568";
        };
    }

    // ── Custom list cell ──────────────────────────────────────────────────
    private class PlayerCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            boolean isSelected = item.startsWith("✓ ");
            setText(item);

            if (isSelected) {
                setStyle("-fx-background-color: #0d2d1a; -fx-text-fill: #27ae60; "
                        + "-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
            } else {
                setStyle("-fx-background-color: transparent; -fx-text-fill: #c8d8e8; "
                        + "-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
            }
        }
    }
}