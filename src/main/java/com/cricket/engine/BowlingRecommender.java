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

/**
 * Generates a 90-over bowling plan with realistic spell rotation:
 *  - Bowlers must rest for at least (spell_length / 2) overs after a spell
 *  - Pace bowlers hard-capped at 6 consecutive overs
 *  - Bottom 25% by adjWPB excluded entirely
 *  - Among eligible candidates, best adjWPB chosen
 */
public class BowlingRecommender {

    private final Map<String, Map<String, Stats>> bowlerStats;
    private final BaselineCalculator baselineCalculator;

    public BowlingRecommender(
            Map<String, Map<String, Stats>> bowlerStats,
            BaselineCalculator baselineCalculator) {
        this.bowlerStats = bowlerStats;
        this.baselineCalculator = baselineCalculator;
    }

    // ── Quality metrics ───────────────────────────────────────────────────

    private double getAdjWPB(String name) {
        Map<String, Stats> m = bowlerStats.getOrDefault(name, new HashMap<>());
        double lhb = m.getOrDefault("LHB", new Stats())
                .getAdjustedWicketsPerBall(baselineCalculator.getLhbWicketsPerBall());
        double rhb = m.getOrDefault("RHB", new Stats())
                .getAdjustedWicketsPerBall(baselineCalculator.getRhbWicketsPerBall());
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

        // Per-bowler state
        Map<String, Integer> currentSpell  = new HashMap<>(); // overs bowled this spell
        Map<String, Integer> restOvers      = new HashMap<>(); // overs rested since last spell
        Map<String, Integer> requiredRest   = new HashMap<>(); // min rest before eligible again

        for (BowlerInfo b : eligible) {
            currentSpell.put(b.getName(), 0);
            restOvers.put(b.getName(), 99);   // start fully rested
            requiredRest.put(b.getName(), 0);
        }

        // Use array so elements can be mutated without effectively-final issues
        String[] ends = new String[2]; // ends[0] = end1, ends[1] = end2

        // Pick opening two pace bowlers (or best two overall)
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
            // Alternate ends each over
            boolean isEnd1Turn = (over % 2 == 1);
            String activeName = isEnd1Turn ? ends[0] : ends[1];
            String otherName  = isEnd1Turn ? ends[1] : ends[0];

            BowlerInfo activeInfo = getInfo(activeName, eligible);
            String role = activeInfo != null ? activeInfo.getRole() : "";

            int spell  = currentSpell.getOrDefault(activeName, 0);
            boolean hardCap = isPaceMedium(role) && spell >= 6;

            // Decide if we need a bowling change at this end
            boolean needChange = hardCap || (over == 81 && isEnd1Turn) || (over == 82 && !isEnd1Turn);

            // Probabilistic change for long spells
            if (!needChange && spell >= 3) {
                double pressure = computePressure(activeName, role, spell, over);
                if (pressure >= 10.0) needChange = true;
            }

            if (needChange) {
                BowlerInfo next = pickNext(eligible, activeName, otherName,
                        currentSpell, restOvers, requiredRest, over);
                if (next != null && !next.getName().equals(activeName)) {
                    // End the current bowler's spell — set required rest
                    int spellLength = currentSpell.getOrDefault(activeName, 0);
                    requiredRest.put(activeName, Math.max(2, spellLength / 2));
                    restOvers.put(activeName, 0);
                    currentSpell.put(activeName, 0);

                    activeName = next.getName();
                    currentSpell.put(activeName, 0);
                }
            }

            plan.assign(over, activeName);

            // Update state
            currentSpell.merge(activeName, 1, Integer::sum);

            // Increment rest counter for non-bowling eligible bowlers
            for (BowlerInfo b : eligible) {
                if (!b.getName().equals(activeName) && !b.getName().equals(otherName)) {
                    restOvers.merge(b.getName(), 1, Integer::sum);
                }
            }

            // Update end assignments
            if (isEnd1Turn) ends[0] = activeName;
            else            ends[1] = activeName;
        }

        return plan;
    }

    // ── Pressure score ────────────────────────────────────────────────────

    private double computePressure(String name, String role, int spell, int over) {
        double adjWPB = getAdjWPB(name);
        double wicketCredit = 5.0 * adjWPB * 6.0;
        double x = -wicketCredit; // good bowlers resist change longer

        if (isPaceMedium(role)) {
            x += 2.0 * spell;
        } else {
            x += 0.4 * spell; // spinners can bowl longer
        }

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
                // Can't be the bowler at either end right now
                .filter(b -> !b.getName().equals(currentBowler))
                .filter(b -> !b.getName().equals(otherEndBowler))
                // Must have rested enough
                .filter(b -> restOvers.getOrDefault(b.getName(), 99)
                             >= requiredRest.getOrDefault(b.getName(), 0))
                // Hard cap: pace/medium can't bowl if at 6 consecutive
                .filter(b -> !(isPaceMedium(b.getRole())
                             && currentSpell.getOrDefault(b.getName(), 0) >= 6))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            // Relax rest requirement and try again
            candidates = eligible.stream()
                    .filter(b -> !b.getName().equals(currentBowler))
                    .filter(b -> !b.getName().equals(otherEndBowler))
                    .collect(Collectors.toList());
        }

        if (candidates.isEmpty()) return getInfo(currentBowler, eligible);

        // New ball: prefer pace/medium
        if (newBall) {
            List<BowlerInfo> pacers = candidates.stream()
                    .filter(b -> isPaceMedium(b.getRole()))
                    .collect(Collectors.toList());
            if (!pacers.isEmpty()) candidates = pacers;
        } else {
            // Middle overs: if replacing a pacer, prefer spinner
            BowlerInfo curr = getInfo(currentBowler, eligible);
            if (curr != null && isPaceMedium(curr.getRole())) {
                List<BowlerInfo> spinners = candidates.stream()
                        .filter(b -> isSpin(b))
                        .collect(Collectors.toList());
                if (!spinners.isEmpty()) candidates = spinners;
            }
        }

        // Best adjWPB among eligible candidates
        return candidates.stream()
                .max(Comparator.comparingDouble(b -> getAdjWPB(b.getName())))
                .orElse(null);
    }

    private BowlerInfo getInfo(String name, List<BowlerInfo> eligible) {
        return eligible.stream()
                .filter(b -> b.getName().equals(name))
                .findFirst().orElse(null);
    }
}