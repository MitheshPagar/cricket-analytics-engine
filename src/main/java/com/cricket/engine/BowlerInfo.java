package com.cricket.engine;

/**
 * Lightweight UI model representing a bowler in the allocation tool.
 */
public class BowlerInfo {

    public enum Category { FAST, MEDIUM_FAST, MEDIUM, SPIN, PART_TIME }

    private final String name;
    private final String role;       // e.g. "RF", "LFM", "ROS"
    private final Category category;

    public BowlerInfo(String name, String role) {
        this.name = name;
        this.role = role;
        this.category = categorise(role);
    }

    private static Category categorise(String role) {
        if (role == null || role.isBlank()) return Category.PART_TIME;
        // Fast
        if (role.equals("RF") || role.equals("LF")) return Category.FAST;
        // Fast-medium
        if (role.equals("RFM") || role.equals("LFM")) return Category.MEDIUM_FAST;
        // Medium / medium-fast (MF/FM combos)
        if (role.equals("RMF") || role.equals("LMF")
                || role.equals("RM") || role.equals("LM")) return Category.MEDIUM;
        // Off-spin / leg-spin
        if (role.equals("ROS") || role.equals("LOS")
                || role.equals("RLS") || role.equals("LLS")) return Category.SPIN;
        return Category.PART_TIME;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public Category getCategory() { return category; }

    /** Short label for grid cells (max 4 chars). */
    public String getShortName() {
        String[] parts = name.split(" ");
        if (parts.length == 1) return name.substring(0, Math.min(4, name.length()));
        // Initials + surname start, e.g. "JJ Bumrah" → "Bum"
        return parts[parts.length - 1].substring(0, Math.min(4, parts[parts.length - 1].length()));
    }

    /** CSS style class for role badge colour. */
    public String getCategoryStyleClass() {
        return switch (category) {
            case FAST -> "badge-fast";
            case MEDIUM_FAST -> "badge-medium-fast";
            case MEDIUM -> "badge-medium";
            case SPIN -> "badge-spin";
            case PART_TIME -> "badge-part-time";
        };
    }

    @Override
    public String toString() { return name; }
}