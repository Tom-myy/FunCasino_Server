package com.example.demoSpringInitializrForEvoBJ.ttimer;

import com.example.demoSpringInitializrForEvoBJ.Game.TimerObserver;
import com.example.demoSpringInitializrForEvoBJ.GameService;
import com.example.demoSpringInitializrForEvoBJ.Message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.example.demoSpringInitializrForEvoBJ.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.TableService;
import com.example.demoSpringInitializrForEvoBJ.myPackage.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;
import org.springframework.stereotype.Component;

@Component
public class DecisionTimeObserver implements TimerObserver {
    private final MessageSender messageSender;
    private final PlayerRegistry playerRegistry;

    public DecisionTimeObserver(MessageSender messageSender, PlayerRegistry playerRegistry) {
        this.messageSender = messageSender;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void timeWasChanged(int seconds) {
        for (Player p : playerRegistry.getPlayersWhoAreInGame()) {
            if (seconds == -1)
                messageSender.sendToClient(p.getPlayerUUID(), new MyPackage<>(seconds, EMessageType.TIMER_CANCEL));
            else
                messageSender.sendToClient(p.getPlayerUUID(), new MyPackage<>(seconds, EMessageType.TIMER));
        }
    }
}
