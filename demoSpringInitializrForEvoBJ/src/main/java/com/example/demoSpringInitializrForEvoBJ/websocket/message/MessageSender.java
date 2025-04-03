package com.example.demoSpringInitializrForEvoBJ.websocket.message;

import com.example.demoSpringInitializrForEvoBJ.websocket.Client;
import com.example.demoSpringInitializrForEvoBJ.websocket.ConnectionStatus;
import com.example.demoSpringInitializrForEvoBJ.websocket.WebSocketClientHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

@Component
public class MessageSender implements IMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final ObjectMapper objectMapper;
    private final WebSocketClientHolder webSocketClientHolder;

    public MessageSender(WebSocketClientHolder webSocketClientHolder, ObjectMapper objectMapper) {
        this.webSocketClientHolder = webSocketClientHolder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendToClient(Client client, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            synchronized (client.getSession()) {
                client.getSession().sendMessage(new TextMessage(responseJson));
            }
            logger.info("SendToClient (" + client.getSession().getId() + " - session ID (not UUID)): msg_json - " + responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {

            } else
                e.printStackTrace();
        }
    }

    @Override
    public void sendToClient(UUID playerUUID, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Client client = webSocketClientHolder.findAuthClientByUUID(playerUUID);

        if (client == null) {
            logger.error("Client not found for UUID: " + playerUUID);
            return;
        }

        if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            synchronized (client.getSession()) {
                client.getSession().sendMessage(new TextMessage(responseJson));
            }
            logger.info("SendToClient (" + playerUUID + "): msg_json - " + responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
                //mb todo smth
            } else
                e.printStackTrace();
        }
    }

    public void sendToClient(WebSocketSession session, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Client client = webSocketClientHolder.findAuthClientBySession(session);

        if (client == null) {
            logger.error("Client with session {} not found...", session);
            return;
        }

        if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            synchronized (client.getSession()) {
                client.getSession().sendMessage(new TextMessage(responseJson));
            }
            logger.info("SendToClient with session ({}): msg_json - {}", session, responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
                //mb todo smth
            } else
                e.printStackTrace();
        }
    }

    @Override
    public void broadcast(MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("Error during up-casting for broadcasting", e);
            return;
        }

        synchronized (webSocketClientHolder.getAuthenticatedClients()) {
            for (Client client : webSocketClientHolder.getAuthenticatedClients().values()) {
                if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages())
                    continue;

                try {
                    synchronized (client.getSession()) {
                        client.getSession().sendMessage(new TextMessage(responseJson));
                    }
                    logger.info("Broadcast to (" + webSocketClientHolder.findAuthUUIDBySession(client.getSession()) + "): msg_json - " + responseJson);
                } catch (Exception e) {
                    if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
                        //mb todo smth
                    } else {
                        e.printStackTrace();
                        logger.error("Error while broadcasting: " + responseJson, e);
                    }
                }
            }
        }
    }
}
