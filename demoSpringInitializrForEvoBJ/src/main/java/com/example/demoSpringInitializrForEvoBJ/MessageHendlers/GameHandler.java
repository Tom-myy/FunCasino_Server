package com.example.demoSpringInitializrForEvoBJ.MessageHendlers;

import com.example.demoSpringInitializrForEvoBJ.*;
import com.example.demoSpringInitializrForEvoBJ.Game.EDecision;
import com.example.demoSpringInitializrForEvoBJ.Game.Table.Seat;
import com.example.demoSpringInitializrForEvoBJ.Message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.WebSocketClientHolder.WebSocketClientHolder;
import com.example.demoSpringInitializrForEvoBJ.myPackage.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.service.EvoUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GameHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameHandler.class);
    private final EvoUserService evoUserService;
    private final ObjectMapper objectMapper;
    private final MessageSender messageSender;
    private final TableService tableService;
    private final GameService gameService;
    private final PlayerRegistry playerRegistry;
    private final WebSocketClientHolder webSocketClientHolder;


    public GameHandler(EvoUserService evoUserService, ObjectMapper objectMapper, MessageSender messageSender, TableService tableService, GameService gameService, PlayerRegistry playerRegistry, WebSocketClientHolder webSocketClientHolder) {
        this.evoUserService = evoUserService;
        this.objectMapper = objectMapper;
        this.messageSender = messageSender;
        this.tableService = tableService;
        this.gameService = gameService;
        this.playerRegistry = playerRegistry;
        this.webSocketClientHolder = webSocketClientHolder;
    }

    /// copy past part, but it's like shortcut
    public Map<String, Client> getTemporaryClients() {
        return webSocketClientHolder.getTemporaryClients();
    }

    public Map<UUID, Client> getAuthenticatedClients() {
        return webSocketClientHolder.getAuthenticatedClients();
    }

    public List<Player> getPlayers() {
        return playerRegistry.getPlayers();
    }

    public void handleRequestToStartGame(MyPackage<?> myPackage, WebSocketSession session) {
        //TODO check if there is such seat number in collection
        //TODO check if this seat belongs to correct client

        UUID clientUUID = webSocketClientHolder.findAuthUUIDBySession(session);
        if (clientUUID == null) { //TODO must be ok, but HANDLE this situation...
            logger.error("clientUUID == null for REQUEST_TO_START_GAME");
            return;
        }

        if (gameService.isGameRunning()) {
            //mb send message kinda "Game is already started"
            messageSender.sendToClient(session, new MyPackage<>(
                    "GAME IS ALREADY STARTED",
                    EMessageType.SOME_VALIDATION_ERROR
            ));
            return;
        }

        if (tableService.isTableReadyToStartGame()) {
            //mb send message kinda "There is no game seat"
            messageSender.sendToClient(session, new MyPackage<>(
                    "THERE IS NO READY SEATS AT THE TABLE FOR GAME",
                    EMessageType.SOME_VALIDATION_ERROR
            ));
            return;
        }

        gameService.processRequestToStartGame(clientUUID);
    }

    public void handleGameDecision(MyPackage<?> myPackage, WebSocketSession session) {
        if (!gameService.isGameRunning()) {
            //mb send message kinda "..."
            messageSender.sendToClient(session, new MyPackage<>(
                    "GAME_DECISION WAS RECEIVED, BUT GAME IS NOT STARTED",
                    EMessageType.SOME_VALIDATION_ERROR
            ));
            return;
        }

        EDecision decision = objectMapper.convertValue(myPackage.getMessage(), EDecision.class);
        gameService.setDecisionField(decision);
    }
}
