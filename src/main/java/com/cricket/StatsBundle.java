package com.cricket;

import java.util.Map;

public class StatsBundle {

    public final Map<String, Map<String, Stats>> batterStats;
    public final Map<String, Map<String, Stats>> bowlerStats;
    public final BaselineCalculator baselineCalculator;
    public final PlayerRoleLoader roleLoader;

    public StatsBundle(
            Map<String, Map<String, Stats>> batterStats,
            Map<String, Map<String, Stats>> bowlerStats,
            BaselineCalculator baselineCalculator,
            PlayerRoleLoader roleLoader
    ) {
        this.batterStats = batterStats;
        this.bowlerStats = bowlerStats;
        this.baselineCalculator = baselineCalculator;
        this.roleLoader = roleLoader;
    }
}