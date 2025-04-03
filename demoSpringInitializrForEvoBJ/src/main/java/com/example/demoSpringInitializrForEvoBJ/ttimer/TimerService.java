package com.example.demoSpringInitializrForEvoBJ.ttimer;

import com.example.demoSpringInitializrForEvoBJ.Game.MyTimer;
import com.example.demoSpringInitializrForEvoBJ.Game.TimerObserver;
import com.example.demoSpringInitializrForEvoBJ.TimerType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*@Component
public class TimerService {

    private final Map<String, MyTimer> timers = new ConcurrentHashMap<>();

    public void start(String key, int seconds, TimerObserver observer) {
        stop(key); // остановим, если уже был

        MyTimer timer = new MyTimer(observer);
        timers.put(key, timer);
        timer.startTimer(seconds);
    }

    public void stop(String key) {
        MyTimer timer = timers.remove(key);
        if (timer != null) {
            timer.stopTimer();
        }
    }

    public void stopAll() {
        timers.values().forEach(MyTimer::stopTimer);
        timers.clear();
    }

    public boolean isRunning(String key) {
        MyTimer timer = timers.get(key);
        return timer != null && timer.isRunning();
    }

}*/

@Component
public class TimerService {

    private final Map<TimerType, MyTimer> timers = new ConcurrentHashMap<>();

    public void start(TimerType type, int seconds, TimerObserver observer) {
        stop(type); // остановим, если уже был

        MyTimer timer = new MyTimer(observer);
        timers.put(type, timer);
        timer.startTimer(seconds);
    }

    public void stop(TimerType type) {
/*        MyTimer timer = timers.remove(type);
        if (timer != null) {
            timer.stopTimer();
        }*/
        timers.remove(type);
        timers.clear();
    }

    public void stopAll() {
        timers.values().forEach(MyTimer::stopTimer);
        timers.clear();
    }

    public boolean isRunning(TimerType type) {
        MyTimer timer = timers.get(type);
        return timer != null && timer.isRunning();
    }

}



    /*@Component
public class TimerService {
    @Getter
    private final MyTimer beforeGameTimer;
    @Getter
    private final MyTimer gameTimer;

    public TimerService(TimerObserver beforeGameObserver, TimerObserver gameObserver) {
        this.beforeGameTimer = new MyTimer(beforeGameObserver);
        this.gameTimer = new MyTimer(gameObserver);
    }
}*/

