package com.cricket.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cricket.BaselineCalculator;
import com.cricket.Stats;

public class BowlingRecommender {

    private final Map<String, Map<String, Stats>> bowlerStats;
    private final BaselineCalculator baselineCalculator;
    private final PitchRecommender pitchRecommender;

    public BowlingRecommender(
            Map<String, Map<String, Stats>> bowlerStats,
            BaselineCalculator baselineCalculator,
            PitchRecommender pitchRecommender) {
        this.bowlerStats       = bowlerStats;
        this.baselineCalculator = baselineCalculator;
        this.pitchRecommender  = pitchRecommender;
    }

    // ── Quality metrics ───────────────────────────────────────────────────

    private double getAdjWPB(String name) {
        Map<String, Stats> m = bowlerStats.getOrDefault(name, new HashMap<>());
        double sharedBaseline = baselineCalculator.getOverallWicketsPerBall();
        double lhb = m.getOrDefault("LHB", new Stats())
                .getAdjustedWicketsPerBall(sharedBaseline);
        double rhb = m.getOrDefault("RHB", new Stats())
                .getAdjustedWicketsPerBall(sharedBaseline);
        return (lhb + rhb) / 2.0;
    }

    // ── Role helpers ──────────────────────────────────────────────────────

    private static boolean isPaceMedium(String role) {
        if (role == null || role.isBlank()) return false;
        return role.equals("RF")  || role.equals("LF")  ||
               role.equals("RFM") || role.equals("LFM") ||
               role.equals("RMF") || role.equals("LMF") ||
               role.equals("RM")  || role.equals("LM");
    }

    private static boolean isSpin(BowlerInfo b) {
        return b.getCategory() == BowlerInfo.Category.SPIN;
    }

    // Whether pitch recommends spin at this over
    private boolean spinRecommendedAt(int over) {
        if (pitchRecommender == null) return over > 25;
        return pitchRecommender.getRecommendedCategories(over)
                .contains(BowlerInfo.Category.SPIN);
    }

    private boolean seamRecommendedAt(int over) {
        if (pitchRecommender == null) return over <= 20;
        List<BowlerInfo.Category> cats = pitchRecommender.getRecommendedCategories(over);
        return cats.contains(BowlerInfo.Category.FAST) ||
               cats.contains(BowlerInfo.Category.MEDIUM_FAST);
    }

    // ── Exclusion ─────────────────────────────────────────────────────────

    private Set<String> computeExcluded(List<BowlerInfo> bowlers) {
        List<BowlerInfo> ranked = new ArrayList<>(bowlers);
        ranked.sort(Comparator.comparingDouble(b -> getAdjWPB(b.getName())));
        int exclude = Math.min(
                (int) Math.floor(bowlers.size() * 0.25),
                Math.max(0, bowlers.size() - 5));
        Set<String> excluded = new HashSet<>();
        for (int i = 0; i < exclude; i++) excluded.add(ranked.get(i).getName());
        return excluded;
    }

    // ── Plan generator ────────────────────────────────────────────────────

    public BowlingPlan generate(List<BowlerInfo> bowlers) {
        BowlingPlan plan = new BowlingPlan();

        Set<String> excluded = computeExcluded(bowlers);
        List<BowlerInfo> eligible = bowlers.stream()
                .filter(b -> !excluded.contains(b.getName()))
                .sorted((a, b) -> Double.compare(getAdjWPB(b.getName()), getAdjWPB(a.getName())))
                .collect(Collectors.toList());

        if (eligible.isEmpty()) eligible = new ArrayList<>(bowlers);

        Map<String, Integer> currentSpell = new HashMap<>();
        Map<String, Integer> restOvers    = new HashMap<>();
        Map<String, Integer> requiredRest = new HashMap<>();

        for (BowlerInfo b : eligible) {
            currentSpell.put(b.getName(), 0);
            restOvers.put(b.getName(), 99);
            requiredRest.put(b.getName(), 0);
        }

        String[] ends = new String[2];

        // Open with best two pace bowlers
        List<BowlerInfo> openers = eligible.stream()
                .filter(b -> isPaceMedium(b.getRole()))
                .limit(2)
                .collect(Collectors.toList());
        if (openers.size() < 2) openers = eligible.stream().limit(2).collect(Collectors.toList());

        ends[0] = !openers.isEmpty() ? openers.get(0).getName() : eligible.get(0).getName();
        if (openers.size() > 1) {
            ends[1] = openers.get(1).getName();
        } else {
            String firstEnd = ends[0];
            ends[1] = eligible.stream()
                    .filter(b -> !b.getName().equals(firstEnd))
                    .map(BowlerInfo::getName)
                    .findFirst().orElse(firstEnd);
        }

        for (int over = 1; over <= 90; over++) {
            boolean isEnd1Turn = (over % 2 == 1);
            String activeName = isEnd1Turn ? ends[0] : ends[1];
            String otherName  = isEnd1Turn ? ends[1] : ends[0];

            BowlerInfo activeInfo = getInfo(activeName, eligible);
            String role = activeInfo != null ? activeInfo.getRole() : "";

            int spell   = currentSpell.getOrDefault(activeName, 0);
            boolean hardCap = isPaceMedium(role) && spell >= 6;

            // Spinners hard-capped at 12 consecutive overs
            boolean spinCap = isSpin(activeInfo) && spell >= 12;

            boolean needChange = hardCap || spinCap;

            // Pressure-based change
            if (!needChange && spell >= 3) {
                double pressure = computePressure(activeName, role, spell, over);
                if (pressure >= 10.0) needChange = true;
            }

            // Force change if pitch doesn't recommend this bowler type at this over
            if (!needChange && spell >= 2) {
                boolean isSpinner = isSpin(activeInfo);
                if (isSpinner && !spinRecommendedAt(over)) needChange = true;
                if (!isSpinner && isPaceMedium(role) && !seamRecommendedAt(over)
                        && spinRecommendedAt(over) && over > 30) needChange = true;
            }

            if (needChange) {
                BowlerInfo next = pickNext(eligible, activeName, otherName,
                        currentSpell, restOvers, requiredRest, over);
                if (next != null && !next.getName().equals(activeName)) {
                    int spellLength = currentSpell.getOrDefault(activeName, 0);
                    requiredRest.put(activeName, Math.max(2, spellLength / 2));
                    restOvers.put(activeName, 0);
                    currentSpell.put(activeName, 0);

                    activeName = next.getName();
                    currentSpell.put(activeName, 0);
                }
            }

            plan.assign(over, activeName);
            currentSpell.merge(activeName, 1, Integer::sum);

            for (BowlerInfo b : eligible) {
                if (!b.getName().equals(activeName) && !b.getName().equals(otherName)) {
                    restOvers.merge(b.getName(), 1, Integer::sum);
                }
            }

            if (isEnd1Turn) ends[0] = activeName;
            else            ends[1] = activeName;
        }

        return plan;
    }

    // ── Pressure score ────────────────────────────────────────────────────

    private double computePressure(String name, String role, int spell, int over) {
        double adjWPB = getAdjWPB(name);
        double wicketCredit = 5.0 * adjWPB * 6.0;
        double x = -wicketCredit;

        if (isPaceMedium(role)) {
            x += 2.0 * spell;   // pacers tire quickly
        } else {
            x += 1.2 * spell;   // spinners also build pressure but slower (was 0.4 — too low)
        }

        // Pitch-aware: if pitch doesn't suit this type, increase pressure to change
        boolean isSpinner = isSpin(getInfoByRole(role));
        if (isSpinner && !spinRecommendedAt(over)) x += 4.0;
        if (!isSpinner && isPaceMedium(role) && !seamRecommendedAt(over)) x += 2.0;

        if (over < 20) x *= 0.75;
        if (x < 1) x = 1;
        return Math.pow(x, 1.5);
    }

    // ── Bowler selection ──────────────────────────────────────────────────

    private BowlerInfo pickNext(
            List<BowlerInfo> eligible,
            String currentBowler,
            String otherEndBowler,
            Map<String, Integer> currentSpell,
            Map<String, Integer> restOvers,
            Map<String, Integer> requiredRest,
            int over
    ) {
        boolean newBall = over <= 10 || over >= 81;

        List<BowlerInfo> candidates = eligible.stream()
                .filter(b -> !b.getName().equals(currentBowler))
                .filter(b -> !b.getName().equals(otherEndBowler))
                .filter(b -> restOvers.getOrDefault(b.getName(), 99)
                             >= requiredRest.getOrDefault(b.getName(), 0))
                .filter(b -> !(isPaceMedium(b.getRole())
                             && currentSpell.getOrDefault(b.getName(), 0) >= 6))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            candidates = eligible.stream()
                    .filter(b -> !b.getName().equals(currentBowler))
                    .filter(b -> !b.getName().equals(otherEndBowler))
                    .collect(Collectors.toList());
        }

        if (candidates.isEmpty()) return getInfo(currentBowler, eligible);

        // New ball: always prefer pace/medium
        if (newBall) {
            List<BowlerInfo> pacers = candidates.stream()
                    .filter(b -> isPaceMedium(b.getRole()))
                    .collect(Collectors.toList());
            if (!pacers.isEmpty()) candidates = pacers;
        } else {
            // Use pitch to decide seam vs spin preference
            boolean spinOk  = spinRecommendedAt(over);
            boolean seamOk  = seamRecommendedAt(over);

            if (seamOk && !spinOk) {
                // Seam pitch — prefer pacers
                List<BowlerInfo> pacers = candidates.stream()
                        .filter(b -> isPaceMedium(b.getRole()))
                        .collect(Collectors.toList());
                if (!pacers.isEmpty()) candidates = pacers;
            } else if (spinOk && !seamOk) {
                // Spin pitch — prefer spinners
                List<BowlerInfo> spinners = candidates.stream()
                        .filter(b -> isSpin(b))
                        .collect(Collectors.toList());
                if (!spinners.isEmpty()) candidates = spinners;
            }
            // If both or neither recommended, use best adjWPB from all candidates
        }

        return candidates.stream()
                .max(Comparator.comparingDouble(b -> getAdjWPB(b.getName())))
                .orElse(null);
    }

    private BowlerInfo getInfo(String name, List<BowlerInfo> eligible) {
        return eligible.stream()
                .filter(b -> b.getName().equals(name))
                .findFirst().orElse(null);
    }

    // Helper for pressure when we only have role string
    private BowlerInfo getInfoByRole(String role) {
        return new BowlerInfo("_tmp", role != null ? role : "");
    }
}