package com.example.demoSpringInitializrForEvoBJ.ttimer;

import com.example.demoSpringInitializrForEvoBJ.Game.TimerObserver;
import com.example.demoSpringInitializrForEvoBJ.Message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.myPackage.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BettingTimeObserver implements TimerObserver {
    private static final ExecutorService messageExecutor = Executors.newCachedThreadPool();
    @Setter
    private Runnable onTimeout;

    private final MessageSender messageSender;

    public BettingTimeObserver(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void timeWasChanged(int seconds) {
        if (seconds == -1 && onTimeout != null) {
            messageExecutor.submit(() -> {
                messageSender.broadcast(new MyPackage<>("", EMessageType.TIMER_CANCEL));//TODO mustn't broadcast - players at the table\in the game
            });

            onTimeout.run();
        }
        else
            messageExecutor.submit(() -> {
                messageSender.broadcast(new MyPackage<>(seconds, EMessageType.TIMER));//TODO mustn't broadcast - players at the table\in the game
            });

    }
}
