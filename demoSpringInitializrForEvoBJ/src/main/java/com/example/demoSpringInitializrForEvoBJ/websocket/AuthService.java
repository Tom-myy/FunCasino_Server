package com.example.demoSpringInitializrForEvoBJ.websocket;

import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.player.dto.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.player.model.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import com.example.demoSpringInitializrForEvoBJ.websocket.handler.AuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final WebSocketClientHolder clientHolder;
    private final PlayerRegistry playerRegistry;

    public AuthService(WebSocketClientHolder clientHolder, PlayerRegistry playerRegistry) {
        this.clientHolder = clientHolder;
        this.playerRegistry = playerRegistry;
    }

    public Player processLogin(Client client, EvoUser evoUser) {
        UUID playerUUID = evoUser.getUserID();
        Client existing = clientHolder.findAuthClientByUUID(playerUUID);

        Player player = playerRegistry.findPlayerByUUID(playerUUID);
        if (player != null) {
            // игрок был в игре → переподключение
            clientHolder.reconnectClient(client, playerUUID);
            logger.info(evoUser.getNickName() + " (" + client.getPlayerUUID() + ") was reconnected successfully");
            return player;
        } else {
            // создаём нового
            Player newPlayer = new Player();
            newPlayer.setEvoUserDTO(convertToDTO(evoUser));
            playerRegistry.addPlayer(newPlayer);
            clientHolder.addAuthenticatedClient(client, newPlayer.getPlayerUUID());
            logger.info("Login successful for user " + newPlayer.getNickName() + " (" + newPlayer.getPlayerUUID() + ")");

            return newPlayer;
        }
    }

    private EvoUserDTO convertToDTO(EvoUser evoUser) {
        return new EvoUserDTO(
                evoUser.getUserID(),
                evoUser.getName(),
                evoUser.getSurname(),
                evoUser.getNickName(),
                evoUser.getBalance()
        );
    }

}
