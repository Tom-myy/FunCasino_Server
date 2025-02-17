package com.example.demoSpringInitializrForEvoBJ.Game;

public class MyTimer {

    private volatile boolean isRunning = false;

    public /*synchronized*/ boolean isRunning() {
        return isRunning;
    }

    public /*synchronized*/ void setRunning(boolean running) {
        isRunning = running;
    }

    public void startTimer(int time) {
        setRunning(true);

        while (time >= 0 && isRunning()) {

            final int currentIteration = time;
            System.out.println("Timer - " + currentIteration);

            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            --time;
        }
        setRunning(false);
    }

    public void stopTimer() {
        setRunning(false);
    }
}