package com.cricket.engine;

public class Partnership {
    public final String batter1;
    public final String batter2;
    public int runs  = 0;
    public int balls = 0;

    public Partnership(String batter1, String batter2) {
        this.batter1 = batter1;
        this.batter2 = batter2;
    }

    public void record(int runs) {
        this.runs += runs;
        this.balls++;
    }
}