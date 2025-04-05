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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketClientHolder webSocketClientHolder;
    private final MessageDispatcher messageDispatcher;
    private final PlayerRegistry playerRegistry;
    private final MessageSender messageSender;
    private final GameService gameService;
    private final TableService tableService;

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
        MyPackage<?> myPackage = parseMessage(message);
        if (myPackage == null) return;

        Client client = resolveClient(session, myPackage);
        if (client == null) return;

        logClientMessage(client, session, myPackage);

        messageDispatcher.dispatch(myPackage, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession client, CloseStatus status) {
        new Thread(() -> {
            removingInactiveClient(client);
        }).start();
    }

    public void removingInactiveClient(WebSocketSession session) {
        Client client = webSocketClientHolder.findAuthClientBySession(session);

        if (webSocketClientHolder.getAuthenticatedClients().containsValue(client)) {
            client.setConnectionStatusToDisconnect();

            while (gameService.isGameRunning()) {

            }

            if (client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                return;
            }

            UUID clientUUID = webSocketClientHolder.findAuthUUIDBySession(session);

            tableService.removePlayersSeatsAtTheTable(playerRegistry.findPlayerByUUID(clientUUID));
            webSocketClientHolder.getAuthenticatedClients().remove(clientUUID);
            playerRegistry.getPlayers().remove(playerRegistry.findPlayerByUUID(clientUUID));
            //сделать некий метод, который после отключения клиента будет проверять списки клиентов,
            //игроков и мест, и будет удалять отключившигося клиента оттуда

            logger.info("Client disconnected: {}", clientUUID);

            messageSender.broadcast(new MyPackage<>(clientCount(), EMessageType.CLIENT_COUNT));
            messageSender.broadcast(new MyPackage<>(tableService.getTable(), EMessageType.TABLE_STATUS));
        }
    }

    private MyPackage<?> parseMessage(TextMessage message) {
        String payload = message.getPayload();
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse client message", e);
            return null;
        }
    }

    private Client resolveClient(WebSocketSession session, MyPackage<?> myPackage) {
        String tempUUID = webSocketClientHolder.findTempUUIDBySession(session);
        UUID authUUID = webSocketClientHolder.findAuthUUIDBySession(session);

        if (tempUUID == null && authUUID == null) {
            logger.error("Client UUID unknown, message type: {}", myPackage.getMessageType());
            return null;
        }

        Client client = (authUUID != null)
                ? webSocketClientHolder.findAuthClientByUUID(authUUID)
                : webSocketClientHolder.findTempClientByUUID(tempUUID);

        if (client == null) {
            logger.error("Client not found by UUID, message type: {}", myPackage.getMessageType());
        }

        return client;
    }

    private void logClientMessage(Client client, WebSocketSession session, MyPackage<?> myPackage) {
        if (client.getPlayerUUID() != null) {
            logger.info("Received ({}) from AUTH client UUID ({})", myPackage.getMessageType(), client.getPlayerUUID());
        } else {
            logger.info("Received ({}) from TEMP client session ID ({})", myPackage.getMessageType(), session.getId());
        }
    }

    private int clientCount() {
        int count = 0;
        for (Client c : webSocketClientHolder.getAuthenticatedClients().values()) {
            if (c.getConnectionStatus() == ConnectionStatus.CONNECTED) ++count;
        }
        return count;
    }
}