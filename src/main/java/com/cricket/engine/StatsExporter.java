package com.cricket.engine;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class StatsExporter {

    public static void export(MonteCarloEngine.SimResult res,
                               String teamAName, String teamBName,
                               int simCount, String outputPath) throws Exception {

        XSSFWorkbook wb = new XSSFWorkbook();

        // ── Shared styles ─────────────────────────────────────────────────
        CellStyle hdr   = headerStyle(wb, new byte[]{(byte)10,(byte)20,(byte)40});
        CellStyle gold  = boldColor(wb, new byte[]{(byte)212,(byte)160,(byte)48});
        CellStyle num   = numStyle(wb);
        CellStyle dec   = decStyle(wb);
        CellStyle name  = nameStyle(wb);
        CellStyle title = titleStyle(wb);

        // ── Sheet 1: Summary ──────────────────────────────────────────────
        XSSFSheet s1 = wb.createSheet("Summary");
        s1.setColumnWidth(0, 5500); s1.setColumnWidth(1, 4500);
        s1.setColumnWidth(2, 4500); s1.setColumnWidth(3, 4500);

        int r = 0;
        setCell(s1, r, 0, teamAName + " vs " + teamBName + " — Monte Carlo Results", title);
        s1.addMergedRegion(new CellRangeAddress(r, r, 0, 3)); r++;
        setCell(s1, r++, 0, "Total simulations: " + simCount, name); r++;

        setCell(s1, r, 0, "",              hdr);
        setCell(s1, r, 1, teamAName,       hdr);
        setCell(s1, r, 2, "Draw",          hdr);
        setCell(s1, r, 3, teamBName,       hdr); r++;

        setCell(s1, r, 0, "Wins / Draws",  name);
        setNumCell(s1, r, 1, res.teamAWins,  gold);
        setNumCell(s1, r, 2, res.draws,      num);
        setNumCell(s1, r, 3, res.teamBWins,  gold); r++;

        setCell(s1, r, 0, "Win %",  name);
        setCell(s1, r, 1, "=B" + r + "/" + simCount + "*100", dec);
        setCell(s1, r, 2, "=C" + r + "/" + simCount + "*100", dec);
        setCell(s1, r, 3, "=D" + r + "/" + simCount + "*100", dec); r += 2;

        setCell(s1, r, 0, "Top Run Scorer",   name);
        setCell(s1, r, 1, res.topRunScorer(),   name);
        s1.addMergedRegion(new CellRangeAddress(r, r, 1, 3)); r++;
        setCell(s1, r, 0, "Top Wicket Taker", name);
        setCell(s1, r, 1, res.topWicketTaker(), name);
        s1.addMergedRegion(new CellRangeAddress(r, r, 1, 3));

        // ── Sheet 2: Batting Stats ────────────────────────────────────────
        XSSFSheet s2 = wb.createSheet("Batting Stats");
        int[] batWidths = {6000,3000,3500,3500,3500,3000,3000,4000};
        for (int c = 0; c < batWidths.length; c++) s2.setColumnWidth(c, batWidths[c]);

        String[] batHdrs = {"Player","Innings","Runs","Average","Strike Rate","100s","50s","Highest Score"};
        writeHeader(s2, 0, batHdrs, hdr);

        List<String> batters = res.batRuns.keySet().stream()
                .sorted((a, b) -> Long.compare(
                        res.batRuns.getOrDefault(b, 0L),
                        res.batRuns.getOrDefault(a, 0L)))
                .collect(Collectors.toList());

        int row = 1;
        for (String p : batters) {
            long runs  = res.batRuns.getOrDefault(p, 0L);
            int  inn   = res.batInnings.getOrDefault(p, 0);
            long balls = res.batBalls.getOrDefault(p, 1L);
            int  hs    = res.batHighest.getOrDefault(p, 0);
            int  h100  = res.batHundreds.getOrDefault(p, 0);
            int  h50   = res.batFifties.getOrDefault(p, 0);

            int er = row + 1; // Excel row (1-indexed)
            setCell(s2, row, 0, p,    name);
            setNumCell(s2, row, 1, inn,  num);
            setNumCell(s2, row, 2, (int) runs, inn >= 100 ? gold : num);
            setCell(s2, row, 3, inn  == 0 ? "-" : String.format("%.2f", runs / (double) inn),  dec);
            setCell(s2, row, 4, balls == 0 ? "-" : String.format("%.2f", runs * 100.0 / balls), dec);
            setNumCell(s2, row, 5, h100, h100 > 0 ? gold : num);
            setNumCell(s2, row, 6, h50,  num);
            setNumCell(s2, row, 7, hs,   hs >= 100 ? gold : num);
            row++;
        }

        // ── Sheet 3: Bowling Stats ────────────────────────────────────────
        XSSFSheet s3 = wb.createSheet("Bowling Stats");
        int[] bowlWidths = {6000,3000,3500,4000,4000,4000,4000,3500,3500,4000};
        for (int c = 0; c < bowlWidths.length; c++) s3.setColumnWidth(c, bowlWidths[c]);

        String[] bowlHdrs = {"Player","Innings","Wickets","Runs Conceded","Balls Bowled",
                             "Average","Strike Rate","5WI","10WM","Best Figures"};
        writeHeader(s3, 0, bowlHdrs, hdr);

        List<String> bowlers = res.bowlWickets.keySet().stream()
                .sorted((a, b) -> Long.compare(
                        res.bowlWickets.getOrDefault(b, 0L),
                        res.bowlWickets.getOrDefault(a, 0L)))
                .collect(Collectors.toList());

        row = 1;
        for (String p : bowlers) {
            long wkts  = res.bowlWickets.getOrDefault(p, 0L);
            long runs2 = res.bowlRuns.getOrDefault(p, 0L);
            long balls2= res.bowlBalls.getOrDefault(p, 1L);
            int  inn2  = res.bowlInnings.getOrDefault(p, 0);
            int  fifers= res.bowlFifers.getOrDefault(p, 0);
            int  tenFor= res.bowlTenFor.getOrDefault(p, 0);
            int[] best = res.bowlBest.getOrDefault(p, new int[]{0,0});

            setCell(s3, row, 0, p,      name);
            setNumCell(s3, row, 1, inn2, num);
            setNumCell(s3, row, 2, (int) wkts, wkts >= 50 ? gold : num);
            setNumCell(s3, row, 3, (int) runs2, num);
            setNumCell(s3, row, 4, (int) balls2, num);
            setCell(s3, row, 5, wkts == 0 ? "-" : String.format("%.2f", runs2 / (double) wkts), dec);
            setCell(s3, row, 6, wkts == 0 ? "-" : String.format("%.2f", balls2 / (double) wkts), dec);
            setNumCell(s3, row, 7, fifers, fifers > 0 ? gold : num);
            setNumCell(s3, row, 8, tenFor, tenFor > 0 ? gold : num);
            setCell(s3, row, 9, best[0] + "-" + best[1], best[0] >= 5 ? gold : num);
            row++;
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) { wb.write(fos); }
        wb.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void writeHeader(XSSFSheet sheet, int rowNum, String[] cols, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        for (int c = 0; c < cols.length; c++) {
            Cell cell = row.createCell(c);
            cell.setCellValue(cols[c]);
            cell.setCellStyle(style);
        }
    }

    private static void setCell(XSSFSheet sheet, int r, int c, String val, CellStyle style) {
        Row row = sheet.getRow(r);
        if (row == null) row = sheet.createRow(r);
        Cell cell = row.createCell(c);
        cell.setCellValue(val);
        if (style != null) cell.setCellStyle(style);
    }

    private static void setNumCell(XSSFSheet sheet, int r, int c, int val, CellStyle style) {
        Row row = sheet.getRow(r);
        if (row == null) row = sheet.createRow(r);
        Cell cell = row.createCell(c);
        cell.setCellValue(val);
        if (style != null) cell.setCellStyle(style);
    }

    // ── Style factories ───────────────────────────────────────────────────

    private static CellStyle headerStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontName("Arial");
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private static CellStyle boldColor(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontName("Arial");
        f.setColor(new XSSFColor(rgb, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static CellStyle numStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setFontName("Arial"); s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static CellStyle decStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setFontName("Arial"); s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static CellStyle nameStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setFontName("Arial"); s.setFont(f);
        return s;
    }

    private static CellStyle titleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontName("Arial"); f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }
}