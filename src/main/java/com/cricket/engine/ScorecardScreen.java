package com.cricket.engine;

import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScorecardScreen {

    private final String teamAName;
    private final String teamBName;
    private final List<InningsResult> innings;
    private final String resultLine;
    private final javafx.stage.Stage primaryStage;
    private final String tossWinner;
    private final String tossDecision;

    public ScorecardScreen(String teamAName, String teamBName,
                           List<InningsResult> innings, String resultLine,
                           javafx.stage.Stage primaryStage) {
        this(teamAName, teamBName, innings, resultLine, primaryStage, null, null);
    }

    public ScorecardScreen(String teamAName, String teamBName,
                           List<InningsResult> innings, String resultLine,
                           javafx.stage.Stage primaryStage,
                           String tossWinner, String tossDecision) {
        this.teamAName    = teamAName;
        this.teamBName    = teamBName;
        this.innings      = innings;
        this.resultLine   = resultLine;
        this.primaryStage = primaryStage;
        this.tossWinner   = tossWinner;
        this.tossDecision = tossDecision;
    }

    public void show() {
        Platform.runLater(() -> {
            Stage stage = (primaryStage != null) ? primaryStage : new Stage();
            stage.setTitle("Match Scorecard — " + teamAName + " vs " + teamBName);

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #0f1923;");

            // ── Header ────────────────────────────────────────────────────
            HBox header = new HBox();
            header.setStyle("-fx-background-color: #0a1218; "
                    + "-fx-border-color: #d4a030; -fx-border-width: 0 0 2 0;");
            header.setPadding(new Insets(16, 24, 16, 24));
            header.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label(teamAName.toUpperCase() + "  vs  " + teamBName.toUpperCase());
            title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 18px; "
                    + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");

            Label result = new Label(resultLine);
            result.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; "
                    + "-fx-text-fill: #27ae60; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(title, spacer, result);

            // ── Tabs: one per innings ──────────────────────────────────────
            TabPane tabs = new TabPane();
            tabs.setStyle("-fx-background-color: #0f1923;");
            tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            String[] inningsLabels = {
                teamAName + " 1st Inn",
                teamBName + " 1st Inn",
                teamAName + " 2nd Inn",
                teamBName + " 2nd Inn"
            };

            for (int i = 0; i < innings.size(); i++) {
                InningsResult ir = innings.get(i);
                String label = i < inningsLabels.length ? inningsLabels[i] : "Innings " + (i + 1);
                Tab tab = new Tab(label, buildInningsTab(ir));
                tab.setStyle("-fx-background-color: #162030; -fx-text-fill: #c8d8e8;");
                tabs.getTabs().add(tab);
            }

            root.setTop(header);
            root.setCenter(tabs);

            stage.setScene(new Scene(root, 1200, 750));
            stage.setResizable(true);
            stage.show();
        });
    }

    // ── Single innings tab ────────────────────────────────────────────────
    private ScrollPane buildInningsTab(InningsResult ir) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #0f1923;");

        // Summary line
        Label summary = new Label(ir.toString());
        summary.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; "
                + "-fx-text-fill: #d4a030; -fx-font-weight: bold;");
        content.getChildren().add(summary);

        // Batting card
        content.getChildren().add(sectionLabel("BATTING"));
        content.getChildren().add(buildBattingTable(ir));

        // Fall of wickets
        if (!ir.getFallOfWickets().isEmpty()) {
            content.getChildren().add(sectionLabel("FALL OF WICKETS"));
            content.getChildren().add(buildFOW(ir));
        }

        // Partnerships
        if (!ir.getPartnerships().isEmpty()) {
            content.getChildren().add(sectionLabel("PARTNERSHIPS"));
            content.getChildren().add(buildPartnerships(ir));
        }

        // Bowling card
        if (!ir.getBowlingCard().isEmpty()) {
            content.getChildren().add(sectionLabel("BOWLING"));
            content.getChildren().add(buildBowlingTable(ir));
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setStyle("-fx-background-color: #0f1923; -fx-background: #0f1923;");
        scroll.setFitToWidth(true);
        return scroll;
    }

    // ── Batting table ──────────────────────────────────────────────────────
    private GridPane buildBattingTable(InningsResult ir) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 0, 4, 0));

        String[] headers = {"Batter", "Dismissal", "R", "B", "4s", "6s", "SR"};
        int[]    widths   = {180, 200, 50, 50, 40, 40, 70};

        for (int c = 0; c < headers.length; c++) {
            Label h = headerLabel(headers[c]);
            h.setPrefWidth(widths[c]);
            grid.add(h, c, 0);
        }

        int row = 1;
        for (BatterRecord b : ir.getBattingCard()) {
            grid.add(cellLabel(b.name, "#c8d8e8"), 0, row);
            grid.add(cellLabel(b.dismissalInfo, "#6a8099"), 1, row);
            grid.add(cellLabel(String.valueOf(b.runs),
                    b.runs >= 100 ? "#d4a030" : b.runs >= 50 ? "#27ae60" : "#c8d8e8"), 2, row);
            grid.add(cellLabel(String.valueOf(b.balls), "#c8d8e8"), 3, row);
            grid.add(cellLabel(String.valueOf(b.fours), "#c8d8e8"), 4, row);
            grid.add(cellLabel(String.valueOf(b.sixes), "#c8d8e8"), 5, row);
            grid.add(cellLabel(String.format("%.1f", b.strikeRate()), "#c8d8e8"), 6, row);
            row++;
        }

        return grid;
    }

    // ── Bowling table ──────────────────────────────────────────────────────
    private GridPane buildBowlingTable(InningsResult ir) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 0, 4, 0));

        String[] headers = {"Bowler", "O", "R", "W", "Econ"};
        int[]    widths   = {180, 50, 60, 40, 70};

        for (int c = 0; c < headers.length; c++) {
            Label h = headerLabel(headers[c]);
            h.setPrefWidth(widths[c]);
            grid.add(h, c, 0);
        }

        int row = 1;
        for (BowlerRecord b : ir.getBowlingCard()) {
            String wColor = b.wickets >= 5 ? "#d4a030"
                          : b.wickets >= 3 ? "#27ae60" : "#c8d8e8";
            grid.add(cellLabel(b.name, "#c8d8e8"), 0, row);
            grid.add(cellLabel(b.overs(), "#c8d8e8"), 1, row);
            grid.add(cellLabel(String.valueOf(b.runsConceded), "#c8d8e8"), 2, row);
            grid.add(cellLabel(String.valueOf(b.wickets), wColor), 3, row);
            grid.add(cellLabel(String.format("%.2f", b.economy()), "#c8d8e8"), 4, row);
            row++;
        }

        return grid;
    }

    // ── Fall of wickets ────────────────────────────────────────────────────
    private VBox buildFOW(InningsResult ir) {
        VBox box = new VBox(4);
        StringBuilder sb = new StringBuilder();
        for (FallOfWicket f : ir.getFallOfWickets()) {
            sb.append(f.wicketNumber).append("-").append(f.score)
              .append(" (").append(f.batterName).append(", ov ")
              .append(f.overs()).append(")   ");
        }
        Label lbl = new Label(sb.toString().trim());
        lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-text-fill: #c8d8e8;");
        lbl.setWrapText(true);
        box.getChildren().add(lbl);
        return box;
    }

    // ── Partnerships ───────────────────────────────────────────────────────
    private GridPane buildPartnerships(InningsResult ir) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(4);

        String[] headers = {"Partnership", "Batters", "Runs", "Balls"};
        int[]    widths   = {100, 300, 60, 60};
        for (int c = 0; c < headers.length; c++) {
            Label h = headerLabel(headers[c]);
            h.setPrefWidth(widths[c]);
            grid.add(h, c, 0);
        }

        int row = 1;
        List<Partnership> ps = ir.getPartnerships();
        for (int i = 0; i < ps.size(); i++) {
            Partnership p = ps.get(i);
            grid.add(cellLabel((i + 1) + getOrdinal(i + 1) + " wkt", "#6a8099"), 0, row);
            grid.add(cellLabel(p.batter1 + " & " + p.batter2, "#c8d8e8"), 1, row);
            grid.add(cellLabel(String.valueOf(p.runs), "#c8d8e8"), 2, row);
            grid.add(cellLabel(String.valueOf(p.balls), "#c8d8e8"), 3, row);
            row++;
        }
        return grid;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-font-weight: bold; -fx-text-fill: #6a8099; "
                + "-fx-border-color: #2a3f55; -fx-border-width: 0 0 1 0; "
                + "-fx-padding: 8 0 4 0;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Label headerLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; "
                + "-fx-font-weight: bold; -fx-text-fill: #d4a030;");
        return l;
    }

    private Label cellLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                + "-fx-text-fill: " + color + ";");
        return l;
    }

    private String getOrdinal(int n) {
        return switch (n) {
            case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th";
        };
    }
}