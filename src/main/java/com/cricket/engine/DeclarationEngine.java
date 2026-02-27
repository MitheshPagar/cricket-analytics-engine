package com.cricket.engine;

import java.util.Map;
import java.util.Random;

public class DeclarationEngine {

    private final Random random = new Random();

    // Baseline runs per over used to estimate what opposition can score
    // in remaining time. Roughly 3.0 rpo for test cricket.
    private static final double TEST_RPO = 3.0;

    /**
     * Called after every ball in a declarable innings.
     *
     * @param inningsNumber     1-based index of this innings in the match (1–4)
     * @param inningsRuns       runs scored so far this innings
     * @param inningsOvers      overs bowled so far this innings (fractional ok)
     * @param lead              current lead (positive = batting team ahead).
     *                          For 1st innings this is just inningsRuns (no lead yet).
     * @param remainingOvers    overs left in the match
     * @param batterScores      map of batter name → runs scored so far this innings
     * @return true if the captain should declare
     */
    public boolean shouldDeclare(
            int inningsNumber,
            int inningsRuns,
            double inningsOvers,
            int lead,
            double remainingOvers,
            Map<String, Integer> batterScores
    ) {

        // 4th innings is never declarable
        if (inningsNumber >= 4) return false;

        // Need at least 1 over bowled before we even consider it
        if (inningsOvers < 1.0) return false;

        // Courtesy rule: don't declare if any batter is between 90–99
        for (int score : batterScores.values()) {
            int mod = score % 100;
            if (mod >= 90 && mod <= 99) {
                // 90% chance to hold back (even the captain gets this courtesy)
                if (random.nextDouble() < 0.9) return false;
            }
        }

        // --- Innings-specific logic ---

        if (inningsNumber == 1) {
            // Declare in 1st innings if very big score or very long innings
            // Probability scales with runs (bigger score = more likely)
            if ((inningsRuns > 500 || inningsOvers > 150)
                    && random.nextDouble() < inningsRuns / 50000.0) {
                return true;
            }
        }

        if (inningsNumber == 2) {
            // Scenario 1: huge total, not too far behind
            if (inningsRuns > 500 && lead > -50
                    && random.nextDouble() < 0.01) {
                return true;
            }

            // Scenario 2: scored more than double opposition's 1st innings,
            // long innings, big enough total
            // Note: we detect this via lead — if lead > inningsRuns/2 approximately.
            // Caller must pass lead = inningsRuns - oppositionFirstInnings.
            // "runs / opp1st > 2" ≡ "inningsRuns > 2 * opp1st"
            // ≡ "inningsRuns - opp1st > opp1st" ≡ "lead > opp1st"
            // But we don't have opp1st directly here, so we approximate:
            // lead > inningsRuns * 0.5 is a reasonable proxy
            if ((double) lead > inningsRuns * 0.5
                    && inningsOvers > 100
                    && inningsRuns > 350
                    && random.nextDouble() < 0.01) {
                return true;
            }

            // Scenario 3: healthy lead, big total
            if (lead > 200 && inningsRuns > 400
                    && random.nextDouble() < 0.01) {
                return true;
            }
        }

        if (inningsNumber == 3) {
            // Dynamic declaration: declare if lead exceeds what opposition
            // could realistically score in remaining time, with a 100-run buffer.
            // Also need at least 35 overs remaining to make bowling them out feasible.
            double expectedOppositionScore = remainingOvers * TEST_RPO;

            if (remainingOvers > 35
                    && lead > expectedOppositionScore + 100) {
                return true;
            }
        }

        return false;
    }
}