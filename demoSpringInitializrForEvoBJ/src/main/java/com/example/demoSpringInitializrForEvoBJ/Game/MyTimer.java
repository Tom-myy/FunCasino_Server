/*
package com.example.demoSpringInitializrForEvoBJ.Game;

import lombok.Getter;

public class MyTimer {

    private TimerObserver observer;
    private volatile boolean isRunning = false;
    @Getter
    private volatile int time = -1;
    private int someX = 0;

    public */
/*synchronized*//*
 boolean isRunning() {
        return isRunning;
    }

    public */
/*synchronized*//*
 void setRunning(boolean running) {
        isRunning = running;
    }

    public MyTimer(*/
/*int time,*//*
 TimerObserver observer) {
//        this.time = time;
        this.observer = observer;
    }

    public void startTimer(int seconds) {
        if (someX == 0) {
            time = seconds;
            notifyObserver(time);
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
                    notifyObserver(time);
            }
//            setRunning(false);
            stopTimer();
//        }).start();
    }

    private void notifyObserver(int timee) {
        observer.timeWasChanged(timee);
    }

    public void stopTimer() {
        setRunning(false);
        someX = 0;
//        time = -1;
//        observer.timeWasChanged(-1);
        notifyObserver(-1);
    }
}*/





















package com.example.demoSpringInitializrForEvoBJ.Game;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyTimer {
    private static final ExecutorService timerExecutor = Executors.newCachedThreadPool();


    private TimerObserver observer;
    @Getter
    private volatile boolean isRunning = false;
    @Getter
    private volatile int time = -1;

//    private Thread timerThread;

    public MyTimer(TimerObserver observer) {
        this.observer = observer;
    }

    public synchronized void startTimer(int seconds) {
        if (isRunning) return;

        isRunning = true;
        time = seconds;

/*        timerThread = new Thread(() -> {
            notifyObserver(time);

            while (time > 0 && isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                time--;
                if (isRunning) notifyObserver(time);
            }

            stopTimer();
        });*/
        timerExecutor.submit(() -> {
            notifyObserver(time);

            while (time > 0 && isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                time--;
                if (isRunning) notifyObserver(time);
            }

            stopTimer();
        });


//        timerThread.start();
    }

    public synchronized void stopTimer() {
        isRunning = false;
        time = -1;
//        if (timerThread != null) {
//            timerThread.interrupt();
//        }
        notifyObserver(-1);
    }

    private void notifyObserver(int time) {
        observer.timeWasChanged(time);
    }
}


/*
public class MyTimer {

    private TimerObserver observer;
    private volatile boolean isRunning = false;
    @Getter
    private volatile int time = -1;
    private int someX = 0;

    public */
/*synchronized*//*
 boolean isRunning() {
        return isRunning;
    }

    public */
/*synchronized*//*
 void setRunning(boolean running) {
        isRunning = running;
    }

    public MyTimer(*/
/*int time,*//*
 TimerObserver observer) {
//        this.time = time;
        this.observer = observer;
    }

    public void startTimer(int seconds) {
        if (someX == 0) {
            time = seconds;
            notifyObserver(time);
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
                notifyObserver(time);
        }
//            setRunning(false);
        stopTimer();
//        }).start();
    }

    private void notifyObserver(int timee) {
        observer.timeWasChanged(timee);
    }

    public void stopTimer() {
        setRunning(false);
        someX = 0;
//        time = -1;
//        observer.timeWasChanged(-1);
        notifyObserver(-1);
    }
}*/
