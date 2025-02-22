package com.example.demoSpringInitializrForEvoBJ.Game;

import lombok.Getter;

public class MyTimer {

    private TimerObserver observer;
    private volatile boolean isRunning = false;
    @Getter
    private volatile int time = -1;
    private int someX = 0;
    private String flagTimerFor = "";

    public /*synchronized*/ boolean isRunning() {
        return isRunning;
    }

    public /*synchronized*/ void setRunning(boolean running) {
        isRunning = running;
    }

    public MyTimer(/*int time,*/ TimerObserver observer) {
//        this.time = time;
        this.observer = observer;
    }

    public void startTimer(int seconds, String flag) {
        if (someX == 0) {
            flagTimerFor = flag;
            time = seconds;
            notifyObserver(time, flagTimerFor);
            ++someX;
        }

//        new Thread(() -> {
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
                if(isRunning())
                    notifyObserver(time, flagTimerFor);
            }
//            setRunning(false);
            stopTimer();
//        }).start();
    }

    private void notifyObserver(int timee, String flag) {
        observer.timeWasChanged(timee, flag);
    }

    public void stopTimer() {
        setRunning(false);
        someX = 0;
//        time = -1;
//        observer.timeWasChanged(-1);
        notifyObserver(-1, flagTimerFor);
        flagTimerFor = "";
    }
}