package com.cricket;

import java.util.List;

import com.cricket.engine.BallEngine;
import com.cricket.engine.InningsEngine;
import com.cricket.engine.PitchProfile;
import com.cricket.engine.TestMatchEngine;

public class TestMatchSimulationMain {

    public static void main(String[] args) throws Exception {

        System.out.println("🏏 Building Stats Pipeline...\n");

        StatsBundle bundle = Main.buildStats();

        PitchProfile basePitch = new PitchProfile(
                1.5,  // green
                1.0,  // dry
                1.5,  // bounce
                1.0,  // flat
                1.0   // boundary
        );

        BallEngine ballEngine = new BallEngine(
                bundle.batterStats,
                bundle.bowlerStats,
                bundle.baselineCalculator,
                basePitch
        );

        InningsEngine inningsEngine =
                new InningsEngine(ballEngine, bundle.roleLoader);

        TestMatchEngine matchEngine =
                new TestMatchEngine(inningsEngine, basePitch);

        List<String> indiaXI = List.of(
                "RG Sharma",
                "Shubman Gill",
                "CA Pujara",
                "V Kohli",
                "AM Rahane",
                "RR Pant",
                "RA Jadeja",
                "R Ashwin",
                "AR Patel",
                "Mohammed Siraj",
                "JJ Bumrah"
        );

        List<String> australiaXI = List.of(
                "DA Warner",
                "UT Khawaja",
                "M Labuschagne",
                "SPD Smith",
                "TM Head",
                "MR Marsh",
                "AT Carey",
                "MA Starc",
                "PJ Cummins",
                "NM Lyon",
                "JR Hazlewood"
        );

        matchEngine.simulateMatch(
                "India",
                indiaXI,
                "Australia",
                australiaXI
        );
    }
}