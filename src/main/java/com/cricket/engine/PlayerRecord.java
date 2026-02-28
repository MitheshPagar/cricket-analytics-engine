package com.cricket.engine;

/**
 * Immutable record of one player loaded from playerRoles.csv.
 */
public class PlayerRecord {

    private final String name;
    private final String batRole;
    private final String bowlRole;

    public PlayerRecord(String name, String batRole, String bowlRole) {
        this.name     = name;
        this.batRole  = batRole;
        this.bowlRole = bowlRole;
    }

    public String getName()     { return name; }
    public String getBatRole()  { return batRole; }
    public String getBowlRole() { return bowlRole; }

    /** Human-readable role summary shown in player picker. */
    public String getRoleSummary() {
        String bat  = batRole.isBlank()  ? "?"  : batRole;
        String bowl = bowlRole.isBlank() ? "—"  : bowlRole;
        return bat + " / " + bowl;
    }

    /** BowlerInfo category derived from bowl role — for badge colouring. */
    public BowlerInfo.Category getBowlerCategory() {
        if (bowlRole == null || bowlRole.isBlank()) return BowlerInfo.Category.PART_TIME;
        if (bowlRole.equals("RF")  || bowlRole.equals("LF"))  return BowlerInfo.Category.FAST;
        if (bowlRole.equals("RFM") || bowlRole.equals("LFM")) return BowlerInfo.Category.MEDIUM_FAST;
        if (bowlRole.equals("RMF") || bowlRole.equals("LMF")
         || bowlRole.equals("RM")  || bowlRole.equals("LM"))  return BowlerInfo.Category.MEDIUM;
        if (bowlRole.equals("ROS") || bowlRole.equals("LOS")
         || bowlRole.equals("RLS") || bowlRole.equals("LLS")) return BowlerInfo.Category.SPIN;
        return BowlerInfo.Category.PART_TIME;
    }

    @Override
    public String toString() { return name; }
}