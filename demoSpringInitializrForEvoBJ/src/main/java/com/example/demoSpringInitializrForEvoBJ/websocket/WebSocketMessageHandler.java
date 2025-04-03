package com.example.demoSpringInitializrForEvoBJ.websocket;

import com.example.demoSpringInitializrForEvoBJ.game.service.GameService;
import com.example.demoSpringInitializrForEvoBJ.game.service.TableService;
import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.websocket.handler.MessageDispatcher;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;

@Component
public class WebSocketMessageHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);
    private final WebSocketClientHolder webSocketClientHolder;
    private final MessageDispatcher messageDispatcher;
    private final PlayerRegistry playerRegistry;
    private final MessageSender messageSender;
    private final GameService gameService;
    private final TableService tableService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketMessageHandler(WebSocketClientHolder webSocketClientHolder, MessageDispatcher messageDispatcher, PlayerRegistry playerRegistry, MessageSender messageSender, GameService gameService, TableService tableService) {
        this.webSocketClientHolder = webSocketClientHolder;
        this.messageDispatcher = messageDispatcher;
        this.playerRegistry = playerRegistry;
        this.messageSender = messageSender;
        this.gameService = gameService;
        this.tableService = tableService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        webSocketClientHolder.addTemporaryClient(session.getId(), new Client(session));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        MyPackage<?> myPackage;
        try {
            myPackage = objectMapper.readValue(payload, new TypeReference<MyPackage<?>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Server got a unknown PACKAGE type!", e);
            return;
        }

        String tempClientUUID = webSocketClientHolder.findTempUUIDBySession(session);
        UUID authClientUUID = webSocketClientHolder.findAuthUUIDBySession(session);

        if (tempClientUUID == null && authClientUUID == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        Client tempClient = null;
        Client authClient = null;

        if (tempClientUUID != null)
            tempClient = webSocketClientHolder.findTempClientByUUID(tempClientUUID);

        if (authClientUUID != null)
            authClient = webSocketClientHolder.findAuthClientByUUID(authClientUUID);

        if (tempClient == null & authClient == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        if (authClient != null) {
            UUID clientUUID = webSocketClientHolder.findAuthUUIDBySession(session);
            logger.info("Received (" + myPackage.getMessageType() + ") message from client with UUID (" + clientUUID + ")");
        } else if (tempClient != null) {
            logger.info("Received (" + myPackage.getMessageType() + ") message from client (" + session.getId() + " - session ID (not UUID))");
        } else {
            logger.error("Oh piece of shit, smth went wrong!");
            return;
        }




        messageDispatcher.dispatch(myPackage, session);
    }

    private int clientCount() {
        int count = 0;
        for (Client c : webSocketClientHolder.getAuthenticatedClients().values()) {
            if (c.getConnectionStatus() == ConnectionStatus.CONNECTED) ++count;
        }
        return count;
    }



    @Override
    public void afterConnectionClosed(WebSocketSession client, CloseStatus status) {
        new Thread(() -> {
            removingInactiveClient(client);
        }).start();
    }

    public void removingInactiveClient(WebSocketSession session) {
        if (webSocketClientHolder.getAuthenticatedClients().containsValue(webSocketClientHolder.findAuthClientBySession(session))) {
            Client client = webSocketClientHolder.findAuthClientBySession(session);
            client.setConnectionStatusToDisconnect();
            while (gameService.isGameRunning()) {

            }

            if (client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                return;
            }
            UUID clientUUID = webSocketClientHolder.findAuthUUIDBySession(session);

            tableService.getTable().removePlayersSeatsAtTheTable(playerRegistry.findPlayerByUUID(clientUUID));
            webSocketClientHolder.getAuthenticatedClients().remove(webSocketClientHolder.findAuthUUIDBySession(session));
            playerRegistry.getPlayers().remove(playerRegistry.findPlayerByUUID(clientUUID));
            //сделать некий метод, который после отключения клиента будет проверять списки клиентов,
            //игроков и мест, и будет удалять отключившигося клиента оттуда

            logger.info("Client disconnected: " + clientUUID);

            messageSender.broadcast(new MyPackage<>(clientCount(), EMessageType.CLIENT_COUNT));
            messageSender.broadcast(new MyPackage<>(tableService.getTable(), EMessageType.TABLE_STATUS));
        }
    }
}