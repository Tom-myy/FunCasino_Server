package com.example.demoSpringInitializrForEvoBJ.timer;

import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
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
