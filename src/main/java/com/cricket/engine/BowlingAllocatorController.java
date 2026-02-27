package com.cricket.engine;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BowlingAllocatorController {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int TOTAL_OVERS   = 90;
    private static final int OVERS_PER_ROW = 10;
    private static final int CELL_W        = 64;
    private static final int CELL_H        = 44;

    // ── State ────────────────────────────────────────────────────────────────
    private final Stage stage;
    private BowlerInfo selectedBowler = null;
    private boolean blockSelectMode   = false;
    private int blockSelectStart      = -1;

    private final BowlingPlan plan             = new BowlingPlan();
    private final PitchRecommender recommender = new PitchRecommender(1.0,1.0,1.0,1.0,1.0);

    // XI data — populated via setXI()
    private final List<BowlerInfo> bowlers = new ArrayList<>();

    // Grid cells indexed by over number (1-based → index 0)
    private final Button[] overCells = new Button[TOTAL_OVERS];

    // Summary rows keyed by bowler name
    private final Map<String, Label> summaryLabels = new LinkedHashMap<>();
    private VBox summaryBox;

    // Status bar
    private Label statusLabel;

    // ── Constructor ──────────────────────────────────────────────────────────
    public BowlingAllocatorController(Stage stage) {
        this.stage = stage;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Set the playing XI. Only players with a recognised bowl role are shown
     * as primary bowlers; others are shown as part-timers.
     */
    public void setXI(List<String> playerNames,
                      Map<String, String> bowlRoles) {
        bowlers.clear();
        for (String name : playerNames) {
            String role = bowlRoles.getOrDefault(name, "");
            bowlers.add(new BowlerInfo(name, role));
        }
    }

    public void show() {
        // Load demo XI if none set
        if (bowlers.isEmpty()) loadDemoXI();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildHeader());
        root.setLeft(buildLeftPanel());
        root.setCenter(buildCenterPanel());
        root.setRight(buildRightPanel());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/bowling-allocator.css"),
                        "CSS not found — using inline styles"
                ) != null
                ? getClass().getResource("/bowling-allocator.css").toExternalForm()
                : ""
        );

        // Inline fallback styles if CSS file not present
        applyInlineStyles(root);

        stage.setTitle("Bowling Allocation — Test Match Planner");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        refreshAll();
    }

    // ── Header ───────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(16);

        Label title = new Label("BOWLING ALLOCATION");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pitchLabel = new Label("Pitch: Neutral");
        pitchLabel.getStyleClass().add("header-pitch");
        pitchLabel.setId("pitchSummaryLabel");

        Button clearBtn = new Button("Clear All");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> clearAll());

        Button exportBtn = new Button("Export Plan");
        exportBtn.getStyleClass().add("btn-primary");
        exportBtn.setOnAction(e -> exportPlan());

        header.getChildren().addAll(title, spacer, pitchLabel, clearBtn, exportBtn);
        return header;
    }

    // ── Left panel — bowler roster + pitch sliders ────────────────────────
    private VBox buildLeftPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("left-panel");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(220);

        Label rosterTitle = new Label("BOWLING XI");
        rosterTitle.getStyleClass().add("panel-title");

        panel.getChildren().add(rosterTitle);

        for (BowlerInfo b : bowlers) {
            panel.getChildren().add(buildBowlerCard(b));
        }

        Separator sep = new Separator();
        sep.getStyleClass().add("separator");

        Label pitchTitle = new Label("PITCH CONDITIONS");
        pitchTitle.getStyleClass().add("panel-title");

        panel.getChildren().addAll(sep, pitchTitle);
        panel.getChildren().addAll(buildPitchSliders());

        return panel;
    }

    private HBox buildBowlerCard(BowlerInfo b) {
        HBox card = new HBox(8);
        card.getStyleClass().addAll("bowler-card", b.getCategoryStyleClass());
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(6, 10, 6, 10));

        Label name = new Label(b.getName());
        name.getStyleClass().add("bowler-name");
        name.setMaxWidth(130);

        Label badge = new Label(b.getRole().isBlank() ? "PT" : b.getRole());
        badge.getStyleClass().addAll("role-badge", b.getCategoryStyleClass() + "-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(name, spacer, badge);

        card.setOnMouseClicked(e -> {
            selectedBowler = b;
            refreshBowlerCards();
            setStatus("Selected: " + b.getName() + " (" + b.getRole() + ")  — click overs to assign");
        });

        card.setId("card-" + b.getName().replace(" ", "_"));
        return card;
    }

    private List<VBox> buildPitchSliders() {
        List<VBox> sliders = new ArrayList<>();
        String[][] conditions = {
            {"Green", "green"},
            {"Dry",   "dry"},
            {"Bounce","bounce"},
            {"Flat",  "flat"},
            {"Boundary","boundary"}
        };

        double[] defaults = {1.0, 1.0, 1.0, 1.0, 1.0};
        double[] values   = defaults.clone();

        for (int i = 0; i < conditions.length; i++) {
            final int idx = i;
            String label = conditions[i][0];

            VBox sliderBox = new VBox(2);
            sliderBox.getStyleClass().add("slider-box");

            HBox labelRow = new HBox();
            labelRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl    = new Label(label);
            lbl.getStyleClass().add("slider-label");
            Label valLbl = new Label(String.format("%.1f", values[idx]));
            valLbl.getStyleClass().add("slider-value");
            valLbl.setId("sliderVal-" + label);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            labelRow.getChildren().addAll(lbl, sp, valLbl);

            Slider slider = new Slider(0.5, 2.0, 1.0);
            slider.getStyleClass().add("pitch-slider");
            slider.setMajorTickUnit(0.5);
            slider.setBlockIncrement(0.1);
            slider.setShowTickMarks(false);

            slider.valueProperty().addListener((obs, oldV, newV) -> {
                values[idx] = newV.doubleValue();
                valLbl.setText(String.format("%.1f", newV.doubleValue()));
                recommender.update(values[0], values[1], values[2], values[3], values[4]);
                refreshGrid();
                updatePitchSummaryLabel();
            });

            sliderBox.getChildren().addAll(labelRow, slider);
            sliders.add(sliderBox);
        }
        return sliders;
    }

    // ── Centre panel — over grid ──────────────────────────────────────────
    private VBox buildCenterPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("center-panel");
        panel.setPadding(new Insets(16));

        // Controls row
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        ToggleButton blockToggle = new ToggleButton("Block Select");
        blockToggle.getStyleClass().add("btn-toggle");
        blockToggle.selectedProperty().addListener((obs, o, n) -> {
            blockSelectMode = n;
            blockSelectStart = -1;
            setStatus(n ? "Block select ON — click first over, then last over of spell"
                        : "Click overs to assign selected bowler");
        });

        Label hint = new Label("Click = assign  •  Right-click = clear  •  Block select = drag spells");
        hint.getStyleClass().add("hint-label");

        controls.getChildren().addAll(blockToggle, hint);

        // Row labels + grid
        GridPane grid = new GridPane();
        grid.getStyleClass().add("over-grid");
        grid.setHgap(4);
        grid.setVgap(4);

        // Column headers
        for (int col = 0; col < OVERS_PER_ROW; col++) {
            Label colHdr = new Label(String.valueOf(col + 1));
            colHdr.getStyleClass().add("grid-col-header");
            colHdr.setPrefWidth(CELL_W);
            colHdr.setAlignment(Pos.CENTER);
            grid.add(colHdr, col + 1, 0);
        }

        // Rows
        int totalRows = (int) Math.ceil((double) TOTAL_OVERS / OVERS_PER_ROW);
        for (int row = 0; row < totalRows; row++) {
            Label rowLbl = new Label((row * OVERS_PER_ROW + 1) + "–"
                    + Math.min((row + 1) * OVERS_PER_ROW, TOTAL_OVERS));
            rowLbl.getStyleClass().add("grid-row-header");
            rowLbl.setPrefWidth(44);
            rowLbl.setAlignment(Pos.CENTER_RIGHT);
            grid.add(rowLbl, 0, row + 1);

            for (int col = 0; col < OVERS_PER_ROW; col++) {
                int overNum = row * OVERS_PER_ROW + col + 1;
                if (overNum > TOTAL_OVERS) break;

                Button cell = buildOverCell(overNum);
                overCells[overNum - 1] = cell;
                grid.add(cell, col + 1, row + 1);
            }
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.getStyleClass().add("grid-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);

        panel.getChildren().addAll(controls, scrollPane);
        return panel;
    }

    private Button buildOverCell(int overNum) {
        Button cell = new Button();
        cell.setPrefSize(CELL_W, CELL_H);
        cell.getStyleClass().add("over-cell");

        cell.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                // Right-click clears
                plan.clear(overNum);
                refreshGrid();
                refreshSummary();
                return;
            }

            if (blockSelectMode) {
                handleBlockSelect(overNum);
                return;
            }

            if (selectedBowler == null) {
                setStatus("Select a bowler from the left panel first.");
                return;
            }

            if (plan.isBackToBack(overNum, selectedBowler.getName())) {
                setStatus("⚠ Back-to-back violation! " + selectedBowler.getShortName()
                        + " just bowled over " + (overNum - 1));
                return;
            }

            plan.assign(overNum, selectedBowler.getName());
            refreshGrid();
            refreshSummary();
        });

        return cell;
    }

    private void handleBlockSelect(int overNum) {
        if (blockSelectStart == -1) {
            blockSelectStart = overNum;
            setStatus("Block start: over " + overNum + " — now click the end over");
            overCells[overNum - 1].getStyleClass().add("cell-block-start");
        } else {
            if (selectedBowler == null) {
                setStatus("Select a bowler first, then use block select.");
                blockSelectStart = -1;
                return;
            }

            int from = Math.min(blockSelectStart, overNum);
            int to   = Math.max(blockSelectStart, overNum);

            // Validate no back-to-back at seam
            boolean violation = false;
            for (int o = from; o <= to; o++) {
                // Within block consecutive is fine (same bowler)
                // Check the over before the block
                if (o == from && plan.isBackToBack(from, selectedBowler.getName())) {
                    violation = true;
                    break;
                }
                // Check the over after the block
                if (o == to) {
                    String nextBowler = plan.getAssignment(to + 1);
                    if (selectedBowler.getName().equals(nextBowler)) {
                        violation = true;
                        break;
                    }
                }
            }

            if (violation) {
                setStatus("⚠ Block creates back-to-back violation at boundary. Adjust range.");
            } else {
                plan.assignBlock(from, to, selectedBowler.getName());
                setStatus("Assigned " + selectedBowler.getName()
                        + " overs " + from + "–" + to
                        + " (" + (to - from + 1) + " overs)");
            }

            // Clear highlight
            overCells[blockSelectStart - 1].getStyleClass().remove("cell-block-start");
            blockSelectStart = -1;
            refreshGrid();
            refreshSummary();
        }
    }

    // ── Right panel — summary ────────────────────────────────────────────
    private VBox buildRightPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("right-panel");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(200);

        Label title = new Label("OVER SUMMARY");
        title.getStyleClass().add("panel-title");

        summaryBox = new VBox(6);
        summaryBox.getStyleClass().add("summary-box");

        for (BowlerInfo b : bowlers) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("summary-row");

            Label nameLbl = new Label(b.getShortName());
            nameLbl.getStyleClass().add("summary-name");
            nameLbl.setPrefWidth(70);

            Label countLbl = new Label("0 ov");
            countLbl.getStyleClass().add("summary-count");
            countLbl.setId("summary-" + b.getName().replace(" ", "_"));
            summaryLabels.put(b.getName(), countLbl);

            row.getChildren().addAll(nameLbl, countLbl);
            summaryBox.getChildren().add(row);
        }

        Separator sep = new Separator();
        Label totalLbl = new Label("Total: 0 / 90");
        totalLbl.getStyleClass().add("summary-total");
        totalLbl.setId("summaryTotal");

        Label legendTitle = new Label("LEGEND");
        legendTitle.getStyleClass().add("panel-title");

        VBox legend = buildLegend();

        Label recommendTitle = new Label("RECOMMENDATIONS");
        recommendTitle.getStyleClass().add("panel-title");

        Label recommendHint = new Label("Gold cells = pitch-recommended\nbowler type for that over");
        recommendHint.getStyleClass().add("hint-label");
        recommendHint.setWrapText(true);

        panel.getChildren().addAll(title, summaryBox, sep, totalLbl,
                new Separator(), legendTitle, legend,
                new Separator(), recommendTitle, recommendHint);
        return panel;
    }

    private VBox buildLegend() {
        VBox legend = new VBox(4);
        String[][] items = {
            {"badge-fast",        "Fast (RF/LF)"},
            {"badge-medium-fast", "Fast-Medium"},
            {"badge-medium",      "Medium"},
            {"badge-spin",        "Spin"},
            {"badge-part-time",   "Part-timer"}
        };
        for (String[] item : items) {
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            Label dot  = new Label("●");
            dot.getStyleClass().addAll("legend-dot", item[0] + "-dot");
            Label lbl  = new Label(item[1]);
            lbl.getStyleClass().add("legend-text");
            row.getChildren().addAll(dot, lbl);
            legend.getChildren().add(row);
        }
        return legend;
    }

    // ── Status bar ────────────────────────────────────────────────────────
    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("status-bar");
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Select a bowler to begin.");
        statusLabel.getStyleClass().add("status-label");
        bar.getChildren().add(statusLabel);
        return bar;
    }

    // ── Refresh logic ─────────────────────────────────────────────────────
    private void refreshAll() {
        refreshBowlerCards();
        refreshGrid();
        refreshSummary();
        updatePitchSummaryLabel();
    }

    private void refreshBowlerCards() {
        for (BowlerInfo b : bowlers) {
            String id = "card-" + b.getName().replace(" ", "_");
            var cardNode = stage.getScene().lookup("#" + id);
            if (cardNode != null) {
                cardNode.getStyleClass().remove("selected");
            }
        }
        if (selectedBowler != null) {
            String id = "card-" + selectedBowler.getName().replace(" ", "_");
            var node = stage.getScene().lookup("#" + id);
            if (node != null && !node.getStyleClass().contains("selected")) {
                node.getStyleClass().add("selected");
            }
        }
    }

    private void refreshGrid() {
        for (int i = 0; i < TOTAL_OVERS; i++) {
            int overNum = i + 1;
            Button cell = overCells[i];
            if (cell == null) continue;

            String assigned = plan.getAssignment(overNum);

            // Clear all state classes
            cell.getStyleClass().removeAll(
                    "cell-assigned", "cell-unassigned", "cell-recommended",
                    "cell-violation", "cell-fast", "cell-medium-fast",
                    "cell-medium", "cell-spin", "cell-part-time"
            );

            if (assigned != null) {
                BowlerInfo bowlerInfo = getBowlerInfo(assigned);
                String text = bowlerInfo != null ? bowlerInfo.getShortName() : assigned;
                cell.setText(text);
                cell.getStyleClass().add("cell-assigned");
                if (bowlerInfo != null) {
                    cell.getStyleClass().add("cell-" + bowlerInfo.getCategory()
                            .name().toLowerCase().replace("_", "-"));
                }

                // Back-to-back check with next over
                if (plan.isBackToBack(overNum + 1, assigned)) {
                    cell.getStyleClass().add("cell-violation");
                }

            } else {
                cell.setText(String.valueOf(overNum));
                cell.getStyleClass().add("cell-unassigned");

                // Highlight if pitch recommends current selected bowler
                if (selectedBowler != null) {
                    List<BowlerInfo.Category> recommended =
                            recommender.getRecommendedCategories(overNum);
                    if (recommended.contains(selectedBowler.getCategory())) {
                        cell.getStyleClass().add("cell-recommended");
                    }
                }
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
                lbl.getStyleClass().removeAll("count-high", "count-medium", "count-low");
                if (count >= 20) lbl.getStyleClass().add("count-high");
                else if (count >= 10) lbl.getStyleClass().add("count-medium");
                else lbl.getStyleClass().add("count-low");
            }
        }
        Label totalLbl = (Label) stage.getScene().lookup("#summaryTotal");
        if (totalLbl != null) totalLbl.setText("Total: " + total + " / 90");
    }

    private void updatePitchSummaryLabel() {
        Label lbl = (Label) stage.getScene().lookup("#pitchSummaryLabel");
        if (lbl != null) lbl.setText("Pitch: " + recommender.getPitchSummary());
    }

    // ── Actions ───────────────────────────────────────────────────────────
    private void clearAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Clear the entire bowling plan?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Clear");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                for (int i = 1; i <= TOTAL_OVERS; i++) plan.clear(i);
                refreshAll();
                setStatus("Plan cleared.");
            }
        });
    }

    private void exportPlan() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Bowling Plan");
        chooser.setInitialFileName("bowling_plan.json");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(plan.toJson());
                setStatus("Plan exported to: " + file.getName());
            } catch (Exception ex) {
                setStatus("Export failed: " + ex.getMessage());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private BowlerInfo getBowlerInfo(String name) {
        return bowlers.stream()
                .filter(b -> b.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void loadDemoXI() {
        Map<String, String> roles = new LinkedHashMap<>();
        roles.put("DA Warner",      "RHB");
        roles.put("UT Khawaja",     "LHB");
        roles.put("M Labuschagne",  "ROS");
        roles.put("SPD Smith",      "RLS");
        roles.put("TM Head",        "LOS");
        roles.put("MR Marsh",       "RMF");
        roles.put("AT Carey",       "RHB");
        roles.put("MA Starc",       "LF");
        roles.put("PJ Cummins",     "RFM");
        roles.put("NM Lyon",        "ROS");
        roles.put("JR Hazlewood",   "RFM");

        for (Map.Entry<String, String> e : roles.entrySet()) {
            bowlers.add(new BowlerInfo(e.getKey(), e.getValue()));
        }
    }

    // ── Inline styles (fallback if CSS file not found) ────────────────────
    private void applyInlineStyles(BorderPane root) {
        // Scoreboard dark theme inline
        root.setStyle(
            "-fx-background-color: #0f1923;" +
            "-fx-font-family: 'Courier New', monospace;"
        );
    }
}