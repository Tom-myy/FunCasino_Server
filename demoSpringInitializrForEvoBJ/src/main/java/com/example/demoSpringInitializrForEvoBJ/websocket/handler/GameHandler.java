package com.example.demoSpringInitializrForEvoBJ.websocket.handler;

import com.example.demoSpringInitializrForEvoBJ.game.EDecision;
import com.example.demoSpringInitializrForEvoBJ.game.service.GameService;
import com.example.demoSpringInitializrForEvoBJ.game.service.TableService;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.websocket.WebSocketClientHolder;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.UUID;

@Component
public class GameHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameHandler.class);
    private final ObjectMapper objectMapper;
    private final MessageSender messageSender;
    private final TableService tableService;
    private final GameService gameService;
    private final WebSocketClientHolder webSocketClientHolder;

    public GameHandler(ObjectMapper objectMapper, MessageSender messageSender, TableService tableService, GameService gameService, WebSocketClientHolder webSocketClientHolder) {
        this.objectMapper = objectMapper;
        this.messageSender = messageSender;
        this.tableService = tableService;
        this.gameService = gameService;
        this.webSocketClientHolder = webSocketClientHolder;
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

        if (!tableService.isTableReadyToStartGame()) {
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