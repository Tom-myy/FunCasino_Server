package com.example.demoSpringInitializrForEvoBJ;

import lombok.Getter;
import org.springframework.stereotype.Component;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
@Component
public class PlayerRegistry {
    private final List<Player> players = new LinkedList<>();//TODO change to Map<UUID, Player> players = new ConcurrentHashMap<>() for productivity

    public void addPlayer(Player player) {
        players.add(player);
    }

    public Player findPlayerByUUID(UUID playerUUID) {
        for (Player player : players) {
            if (player.getPlayerUUID().equals(playerUUID)) {
                return player;
            }
        }
        return null;
    }

    public List<Player> getPlayersWhoAreInGame() {
        List<Player> result = new LinkedList<>();
        for (Player p : players) {
            if(p.isInTheGame())
                result.add(p);
        }

        return result;
    }
}