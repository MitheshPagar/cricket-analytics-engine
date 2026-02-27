package com.cricket.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the complete over-by-over bowling plan for one innings.
 * Can be exported as a Java-compatible List<String> or JSON string.
 */
public class BowlingPlan {

    private static final int MAX_OVERS = 90;

    // Index 0 = over 1. Null means unassigned.
    private final String[] assignments = new String[MAX_OVERS];

    public void assign(int overNumber, String bowlerName) {
        if (overNumber < 1 || overNumber > MAX_OVERS) return;
        assignments[overNumber - 1] = bowlerName;
    }

    public void assignBlock(int fromOver, int toOver, String bowlerName) {
        for (int i = fromOver; i <= toOver; i++) assign(i, bowlerName);
    }

    public void clear(int overNumber) {
        assign(overNumber, null);
    }

    public String getAssignment(int overNumber) {
        if (overNumber < 1 || overNumber > MAX_OVERS) return null;
        return assignments[overNumber - 1];
    }

    public boolean isAssigned(int overNumber) {
        return getAssignment(overNumber) != null;
    }

    /**
     * Returns true if assigning this bowler to this over would create
     * a back-to-back violation (same bowler bowled the previous over).
     */
    public boolean isBackToBack(int overNumber, String bowlerName) {
        if (overNumber <= 1) return false;
        String prev = getAssignment(overNumber - 1);
        return bowlerName != null && bowlerName.equals(prev);
    }

    /** Count overs assigned to a bowler. */
    public int getOverCount(String bowlerName) {
        int count = 0;
        for (String a : assignments) {
            if (bowlerName.equals(a)) count++;
        }
        return count;
    }

    /** Total assigned overs. */
    public int getTotalAssigned() {
        int count = 0;
        for (String a : assignments) {
            if (a != null) count++;
        }
        return count;
    }

    /** All unassigned over numbers (1-based). */
    public List<Integer> getUnassignedOvers() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < MAX_OVERS; i++) {
            if (assignments[i] == null) result.add(i + 1);
        }
        return result;
    }

    /** Map of bowler name → over count for summary panel. */
    public Map<String, Integer> getOverCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String a : assignments) {
            if (a != null) counts.merge(a, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Export as ordered list of bowler names (one per over, up to lastOver).
     * Unassigned overs fall back to the most recent assigned bowler.
     * This is the format InningsEngine expects.
     */
    public List<String> toOrderedBowlingList(int lastOver) {
        List<String> result = new ArrayList<>();
        String lastBowler = null;
        for (int i = 0; i < Math.min(lastOver, MAX_OVERS); i++) {
            String bowler = assignments[i] != null ? assignments[i] : lastBowler;
            if (bowler != null) result.add(bowler);
            if (assignments[i] != null) lastBowler = assignments[i];
        }
        return result;
    }

    /** Export as JSON string for saving/loading plans. */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{\n  \"overs\": [\n");
        for (int i = 0; i < MAX_OVERS; i++) {
            sb.append("    { \"over\": ").append(i + 1)
              .append(", \"bowler\": ")
              .append(assignments[i] == null ? "null" : "\"" + assignments[i] + "\"")
              .append(" }");
            if (i < MAX_OVERS - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}