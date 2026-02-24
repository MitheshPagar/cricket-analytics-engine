package com.cricket.engine;

import java.util.Map;
import java.util.Random;

import com.cricket.BaselineCalculator;
import com.cricket.Stats;

public class BallEngine {

    private final Map<String, Map<String, Stats>> batterStats;
    private final Map<String, Map<String, Stats>> bowlerStats;
    private final BaselineCalculator baselineCalculator;
    private PitchProfile pitch;

    private final Random random = new Random();

    public BallEngine(
            Map<String, Map<String, Stats>> batterStats,
            Map<String, Map<String, Stats>> bowlerStats,
            BaselineCalculator baselineCalculator,
            PitchProfile pitch
    ) {
        this.batterStats = batterStats;
        this.bowlerStats = bowlerStats;
        this.baselineCalculator = baselineCalculator;
        this.pitch = pitch;
    }

    public BallOutcome simulateBall(
            String batter,
            String bowler,
            String bowlRole,
            String batterHand
    ) {

        Stats batStats = batterStats
                .getOrDefault(batter, Map.of())
                .getOrDefault(bowlRole, new Stats());

        Stats bowlStats = bowlerStats
                .getOrDefault(bowler, Map.of())
                .getOrDefault(batterHand, new Stats());

        double baselineRPB = baselineCalculator.getBaselineRunsPerBallForRole(bowlRole);
        double baselineWPB = baselineCalculator.getBaselineWicketsPerBallForRole(bowlRole);

        double batterRPB = batStats.getAdjustedRunsPerBall(baselineRPB);
        double batterWPB = batStats.getAdjustedWicketsPerBall(baselineWPB);

        double bowlerRPB = bowlStats.getAdjustedRunsPerBall(baselineRPB);
        double bowlerWPB = bowlStats.getAdjustedWicketsPerBall(baselineWPB);

        double finalRPB = (batterRPB + bowlerRPB) / 2.0;
        double finalWPB = (batterWPB + bowlerWPB) / 2.0;

        //Pitch Modifiers
        boolean isFast = bowlRole.contains("F");
        boolean isSpin = bowlRole.contains("S");

        double fastInfluence = 0.2;
        double spinInfluence = 0.3;

        if(isFast){
            double seamCondition = (pitch.getGreen() + pitch.getBounce()) / 2.0;
            finalWPB *= 1 + (seamCondition - 1) * fastInfluence;
        }


        if(isSpin){
            finalWPB *= 1 + (pitch.getDry()-1) * spinInfluence;
        }

        finalWPB *= 1 - (pitch.getFlat() - 1) * 0.4;
        finalRPB *= 1 + (pitch.getFlat() - 1) * 0.4;

        double dotAdjusment = 1.0;

        if(isFast){
            dotAdjusment *= pitch.getGreen();
        }

        if(isSpin){
            dotAdjusment *= pitch.getDry();
        }


        finalRPB = clamp(finalRPB, 0.2, 2.0);
        finalWPB = clamp(finalWPB, 0.01, 0.20);

        double r = random.nextDouble();


        if (r < finalWPB) {
            return BallOutcome.WICKET;
        }


        double runRand = random.nextDouble();

        
        double dotProb = 0.57 * dotAdjusment;

        if(isFast){
            double seamCondition = (pitch.getGreen() + pitch.getBounce()) / 2.0;
            dotProb *= 1 + (seamCondition - 1) * 0.25;
        }

        if(isSpin){
            dotProb *= 1 + (pitch.getDry() - 1) * 0.08;
        }

        dotProb *= 1 - (pitch.getFlat() - 1) * 0.3;
        dotProb = clamp(dotProb, 0.3, 0.75);

        double oneProb = 0.25;
        double twoProb = 0.08;

        double boundaryMultiplier = 1 + (pitch.getBoundary() - 1) * 0.4;

        if(isFast){
            double seamCondition = (pitch.getGreen() + pitch.getBounce())/2.0;
            boundaryMultiplier *= 1 - (seamCondition - 1) * 0.1;
        }

        double fourProb = 0.09 * (finalRPB / 1.0) * boundaryMultiplier;
        double sixProb = 0.02 * (finalRPB / 1.2) * boundaryMultiplier;

        if(isSpin){
            fourProb *= 1 - (pitch.getDry() - 1) * 0.2;
            sixProb *= 1 - (pitch.getDry() - 1) * 0.3;
        }

        double totalProb = dotProb + oneProb + twoProb + fourProb + sixProb;

        dotProb /= totalProb;
        oneProb /= totalProb;
        twoProb /= totalProb;
        fourProb /= totalProb;
        sixProb /= totalProb;

        double cumulative = dotProb;
        if (runRand < cumulative) return BallOutcome.DOT;

        cumulative += oneProb;
        if (runRand < cumulative) return BallOutcome.ONE;

        cumulative += twoProb;
        if (runRand < cumulative) return BallOutcome.TWO;

        cumulative += fourProb;
        if (runRand < cumulative) return BallOutcome.FOUR;

        cumulative += sixProb;
        if (runRand < cumulative) return BallOutcome.SIX;

        return BallOutcome.DOT;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void setPitch(PitchProfile pitch) {
        this.pitch = pitch;
    }
}