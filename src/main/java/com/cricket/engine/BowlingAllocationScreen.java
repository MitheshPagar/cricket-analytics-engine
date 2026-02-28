package com.cricket.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BowlingAllocationScreen {

    private static final int TOTAL_OVERS   = 90;
    private static final int OVERS_PER_ROW = 10;
    private static final int CELL_W        = 62;
    private static final int CELL_H        = 42;

    private final String screenTitle;
    private final List<String> bowlingAtXI;  // XI being bowled at (for display)
    private final MatchConfig config;
    private List<BowlerInfo> bowlerInfoList;  // bowling team XI with roles
    private final Consumer<BowlingPlan> onNext;
    private final Runnable onBack;

    private final BowlingPlan plan             = new BowlingPlan();
    private final PitchRecommender recommender = new PitchRecommender(1,1,1,1,1);

    private BowlerInfo selectedBowler = null;
    private boolean blockSelectMode   = false;
    private int blockSelectStart      = -1;

    private final Button[] overCells     = new Button[TOTAL_OVERS];
    private final Map<String, Label> summaryLabels = new HashMap<>();
    private Label statusLabel;
    private Stage stage;

    private List<BowlerInfo> bowlers;

    public BowlingAllocationScreen(String screenTitle,
                                   List<String> bowlingAtXI,
                                   MatchConfig config,
                                   Consumer<BowlingPlan> onNext,
                                   Runnable onBack) {
        this.screenTitle = screenTitle;
        this.bowlingAtXI = bowlingAtXI;
        this.config      = config;
        this.onNext      = onNext;
        this.onBack      = onBack;
    }

    /** Called by BowlingAllocatorApp to inject bowler roles from the database. */
    public void setBowlerInfoList(List<BowlerInfo> bowlerInfoList) {
        this.bowlerInfoList = bowlerInfoList;
    }

    public void show(Stage stage) {
        this.stage = stage;

        // Use pre-built BowlerInfo list (with roles) if provided by BowlingAllocatorApp
        if (bowlerInfoList != null) {
            bowlers = bowlerInfoList;
        } else {
            bowlers = bowlingAtXI.stream()
                    .map(name -> new BowlerInfo(name, ""))
                    .toList();
        }

        // Sync pitch recommender with config pitch
        if (config.pitchProfile != null) {
            recommender.update(
                    config.pitchProfile.getGreen(),
                    config.pitchProfile.getDry(),
                    config.pitchProfile.getBounce(),
                    config.pitchProfile.getFlat(),
                    config.pitchProfile.getBoundary()
            );
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1923;");

        root.setTop(buildHeader());
        root.setLeft(buildLeftPanel());
        root.setCenter(buildCenterPanel());
        root.setRight(buildRightPanel());
        root.setBottom(buildFooter());

        stage.setScene(new Scene(root, 1280, 820));
        stage.show();

        refreshGrid();
    }

    // ── Header ───────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #0a1218; -fx-border-color: #d4a030; -fx-border-width: 0 0 2 0;");
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(screenTitle.toUpperCase());
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pitchLbl = new Label("Pitch: " + recommender.getPitchSummary());
        pitchLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #6a8099;");

        header.getChildren().addAll(title, spacer, pitchLbl);
        return header;
    }

    // ── Left panel: bowler roster ─────────────────────────────────────────
    private VBox buildLeftPanel() {
        VBox panel = new VBox(8);
        panel.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; -fx-border-width: 0 1 0 0;");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(210);

        Label title = new Label("BOWLING XI");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");
        panel.getChildren().add(title);

        for (BowlerInfo b : bowlers) {
            HBox card = new HBox(8);
            String accentColor = accentFor(b.getCategory());
            card.setStyle("-fx-background-color: #1e2d3e; "
                    + "-fx-border-color: " + accentColor + " #2a3f55 #2a3f55 " + accentColor + "; "
                    + "-fx-border-width: 1 1 1 3; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
            card.setId("card-" + b.getName().replace(" ", "_"));
            card.setAlignment(Pos.CENTER_LEFT);

            Label nameLbl = new Label(b.getName());
            nameLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #c8d8e8;");
            nameLbl.setMaxWidth(120);

            Label badge = new Label(b.getRole().isBlank() ? "PT" : b.getRole());
            badge.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: white; "
                    + "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 5 2 5;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            card.getChildren().addAll(nameLbl, sp, badge);

            card.setOnMouseClicked(e -> {
                selectedBowler = b;
                refreshCardHighlights();
                refreshGrid();
                setStatus("Selected: " + b.getName() + "  — click overs to assign");
            });

            panel.getChildren().add(card);
        }

        return panel;
    }

    // ── Centre panel: over grid ───────────────────────────────────────────
    private VBox buildCenterPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #0f1923;");
        panel.setPadding(new Insets(16));

        // Controls row
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        ToggleButton blockToggle = new ToggleButton("Block Select");
        blockToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #6a8099; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-font-family: 'Courier New'; -fx-padding: 5 12 5 12;");
        blockToggle.selectedProperty().addListener((obs, o, n) -> {
            blockSelectMode  = n;
            blockSelectStart = -1;
            blockToggle.setStyle("-fx-background-color: " + (n ? "#1a3050" : "transparent")
                    + "; -fx-text-fill: " + (n ? "#d4a030" : "#6a8099")
                    + "; -fx-border-color: " + (n ? "#d4a030" : "#2a3f55")
                    + "; -fx-border-width: 1; -fx-cursor: hand; "
                    + "-fx-font-family: 'Courier New'; -fx-padding: 5 12 5 12;");
            setStatus(n ? "Block select ON — click first over, then last over of spell"
                        : "Click overs to assign  •  Right-click to clear");
        });

        Label hint = new Label("Click = assign  •  Right-click = clear  •  Gold border = pitch recommended");
        hint.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #6a8099; -fx-font-style: italic;");

        controls.getChildren().addAll(blockToggle, hint);

        // Grid
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);

        // Column headers
        for (int col = 0; col < OVERS_PER_ROW; col++) {
            Label h = new Label(String.valueOf(col + 1));
            h.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #6a8099;");
            h.setPrefWidth(CELL_W);
            h.setAlignment(Pos.CENTER);
            grid.add(h, col + 1, 0);
        }

        int totalRows = (int) Math.ceil((double) TOTAL_OVERS / OVERS_PER_ROW);
        for (int row = 0; row < totalRows; row++) {
            Label rowLbl = new Label((row * OVERS_PER_ROW + 1) + "–"
                    + Math.min((row + 1) * OVERS_PER_ROW, TOTAL_OVERS));
            rowLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 9px; -fx-text-fill: #6a8099;");
            rowLbl.setPrefWidth(44);
            rowLbl.setAlignment(Pos.CENTER_RIGHT);
            grid.add(rowLbl, 0, row + 1);

            for (int col = 0; col < OVERS_PER_ROW; col++) {
                int overNum = row * OVERS_PER_ROW + col + 1;
                if (overNum > TOTAL_OVERS) break;
                Button cell = buildCell(overNum);
                overCells[overNum - 1] = cell;
                grid.add(cell, col + 1, row + 1);
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0f1923;");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(580);

        statusLabel = new Label("Select a bowler from the left panel first.");
        statusLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #6a8099;");

        panel.getChildren().addAll(controls, scroll, statusLabel);
        return panel;
    }

    private Button buildCell(int overNum) {
        Button cell = new Button(String.valueOf(overNum));
        cell.setPrefSize(CELL_W, CELL_H);
        cell.setStyle(unassignedStyle());

        cell.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                plan.clear(overNum);
                refreshGrid();
                refreshSummary();
                return;
            }
            if (blockSelectMode) { handleBlock(overNum); return; }
            if (selectedBowler == null) { setStatus("Select a bowler first."); return; }
            if (plan.isBackToBack(overNum, selectedBowler.getName())) {
                setStatus("⚠  Back-to-back! " + selectedBowler.getName()
                        + " just bowled over " + (overNum - 1));
                return;
            }
            plan.assign(overNum, selectedBowler.getName());
            refreshGrid();
            refreshSummary();
        });
        return cell;
    }

    private void handleBlock(int overNum) {
        if (blockSelectStart == -1) {
            blockSelectStart = overNum;
            setStatus("Block start: over " + overNum + " — now click the end over");
        } else {
            if (selectedBowler == null) {
                setStatus("Select a bowler first.");
                blockSelectStart = -1;
                return;
            }
            int from = Math.min(blockSelectStart, overNum);
            int to   = Math.max(blockSelectStart, overNum);
            if (plan.isBackToBack(from, selectedBowler.getName())) {
                setStatus("⚠  Back-to-back violation at start of block.");
            } else {
                plan.assignBlock(from, to, selectedBowler.getName());
                setStatus("Assigned " + selectedBowler.getName()
                        + "  overs " + from + "–" + to);
            }
            blockSelectStart = -1;
            refreshGrid();
            refreshSummary();
        }
    }

    // ── Right panel: summary ──────────────────────────────────────────────
    private VBox buildRightPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #162030; -fx-border-color: #2a3f55; -fx-border-width: 0 0 0 1;");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(190);

        Label title = new Label("OVER SUMMARY");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");
        panel.getChildren().add(title);

        for (BowlerInfo b : bowlers) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLbl = new Label(b.getShortName());
            nameLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #c8d8e8;");
            nameLbl.setPrefWidth(70);

            Label countLbl = new Label("0 ov");
            countLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #6a8099; -fx-font-weight: bold;");
            countLbl.setId("sum-" + b.getName().replace(" ", "_"));
            summaryLabels.put(b.getName(), countLbl);

            row.getChildren().addAll(nameLbl, countLbl);
            panel.getChildren().add(row);
        }

        panel.getChildren().add(new Separator());

        Label totalLbl = new Label("Total: 0 / 90");
        totalLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #c8d8e8; -fx-font-weight: bold;");
        totalLbl.setId("sumTotal");
        panel.getChildren().add(totalLbl);

        panel.getChildren().add(new Separator());

        Label legendTitle = new Label("LEGEND");
        legendTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099;");
        panel.getChildren().add(legendTitle);

        String[][] legend = {
            {"#c0392b", "Fast (RF/LF)"},
            {"#e67e22", "Fast-Medium"},
            {"#b8860b", "Medium"},
            {"#27ae60", "Spin"},
            {"#4a5568", "Part-timer"},
            {"#d4a030", "Recommended (pitch)"}
        };
        for (String[] entry : legend) {
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + entry[0] + "; -fx-font-size: 14px;");
            Label lbl = new Label(entry[1]);
            lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-text-fill: #6a8099;");
            row.getChildren().addAll(dot, lbl);
            panel.getChildren().add(row);
        }

        return panel;
    }

    // ── Footer: Back / Next ───────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: #0a1218; -fx-border-color: #2a3f55; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(14, 24, 14, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setSpacing(12);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6a8099; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-font-family: 'Courier New'; -fx-padding: 8 20 8 20;");
        backBtn.setOnAction(e -> onBack.run());

        Button nextBtn = new Button("Next →");
        nextBtn.setStyle("-fx-background-color: #d4a030; -fx-text-fill: #0f1923; "
                + "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 8 20 8 20;");
        nextBtn.setOnAction(e -> onNext.accept(plan));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(backBtn, spacer, nextBtn);
        return footer;
    }

    // ── Refresh ───────────────────────────────────────────────────────────
    private void refreshGrid() {
        for (int i = 0; i < TOTAL_OVERS; i++) {
            int overNum = i + 1;
            Button cell = overCells[i];
            if (cell == null) continue;

            String assigned = plan.getAssignment(overNum);

            if (assigned != null) {
                BowlerInfo b = getBowler(assigned);
                String color = b != null ? accentFor(b.getCategory()) : "#6a8099";
                String bg    = b != null ? bgFor(b.getCategory()) : "#1e2d3e";
                cell.setText(b != null ? b.getShortName() : assigned);
                cell.setStyle("-fx-background-color: " + bg + "; "
                        + "-fx-border-color: " + color + "; -fx-border-width: 1; "
                        + "-fx-text-fill: " + color + "; -fx-font-weight: bold; "
                        + "-fx-font-family: 'Courier New'; -fx-font-size: 10px; "
                        + (plan.isBackToBack(overNum + 1, assigned)
                            ? "-fx-border-color: #ff0000; -fx-border-width: 2;" : ""));
            } else {
                cell.setText(String.valueOf(overNum));
                boolean recommended = selectedBowler != null
                        && recommender.getRecommendedCategories(overNum)
                                      .contains(selectedBowler.getCategory());
                cell.setStyle(recommended ? recommendedStyle() : unassignedStyle());
            }
        }
    }

    private void refreshSummary() {
        Map<String, Integer> counts = plan.getOverCounts();
        int total = 0;
        for (BowlerInfo b : bowlers) {
            int count = counts.getOrDefault(b.getName(), 0);
            total += count;
            Label lbl = summaryLabels.get(b.getName());
            if (lbl != null) {
                lbl.setText(count + " ov");
                String color = count >= 20 ? "#d4a030" : count >= 10 ? "#27ae60" : "#6a8099";
                lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                        + "-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        }
        Label totalLbl = (Label) stage.getScene().lookup("#sumTotal");
        if (totalLbl != null) totalLbl.setText("Total: " + total + " / 90");
    }

    private void refreshCardHighlights() {
        for (BowlerInfo b : bowlers) {
            var node = stage.getScene().lookup(
                    "#card-" + b.getName().replace(" ", "_"));
            if (node == null) continue;
            String accent = accentFor(b.getCategory());
            boolean selected = selectedBowler != null
                    && b.getName().equals(selectedBowler.getName());
            node.setStyle("-fx-background-color: " + (selected ? "#1a3050" : "#1e2d3e") + "; "
                    + "-fx-border-color: " + (selected ? "#d4a030" : accent)
                    + " #2a3f55 #2a3f55 " + accent + "; "
                    + "-fx-border-width: 1 1 1 3; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    // ── Style helpers ─────────────────────────────────────────────────────
    private String unassignedStyle() {
        return "-fx-background-color: #1e2d3e; -fx-border-color: #2a3f55; "
                + "-fx-border-width: 1; -fx-text-fill: #3a5570; "
                + "-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-cursor: hand;";
    }

    private String recommendedStyle() {
        return "-fx-background-color: #1e2d3e; -fx-border-color: #d4a030; "
                + "-fx-border-width: 1.5; -fx-text-fill: #d4a030; "
                + "-fx-font-family: 'Courier New'; -fx-font-size: 10px; -fx-cursor: hand;";
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

    private String bgFor(BowlerInfo.Category cat) {
        return switch (cat) {
            case FAST        -> "#3d1515";
            case MEDIUM_FAST -> "#3d2510";
            case MEDIUM      -> "#2e2a00";
            case SPIN        -> "#0d2d1a";
            case PART_TIME   -> "#1e2530";
        };
    }

    private BowlerInfo getBowler(String name) {
        return bowlers.stream().filter(b -> b.getName().equals(name))
                .findFirst().orElse(null);
    }
}