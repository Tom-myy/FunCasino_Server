package com.example.demoSpringInitializrForEvoBJ.websocket.handler;

import com.example.demoSpringInitializrForEvoBJ.websocket.AuthService;
import com.example.demoSpringInitializrForEvoBJ.websocket.Client;
import com.example.demoSpringInitializrForEvoBJ.player.dto.LoginRequestDTO;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.game.service.TableService;
import com.example.demoSpringInitializrForEvoBJ.websocket.WebSocketClientHolder;
import com.example.demoSpringInitializrForEvoBJ.player.model.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.player.service.EvoUserService;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Optional;

@Component
public class AuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final EvoUserService evoUserService;
    private final ObjectMapper objectMapper;
    private final MessageSender messageSender;
    private final TableService tableService;
    private final AuthService authService;
    private final PlayerRegistry playerRegistry;
    private final WebSocketClientHolder webSocketClientHolder;

    public AuthHandler(EvoUserService evoUserService, ObjectMapper objectMapper, MessageSender messageSender, TableService tableService, AuthService authService, PlayerRegistry playerRegistry, WebSocketClientHolder webSocketClientHolder) {
        this.evoUserService = evoUserService;
        this.objectMapper = objectMapper;
        this.messageSender = messageSender;
        this.tableService = tableService;
        this.authService = authService;
        this.playerRegistry = playerRegistry;
        this.webSocketClientHolder = webSocketClientHolder;
    }

    public void handleAuthorization(MyPackage<?> myPackage, WebSocketSession session) {
        Client client = webSocketClientHolder.findTempClientBySession(session);

        if (client == null) {
            logger.error("client == null in AUTHORIZATION");
            return;
        }

        LoginRequestDTO loginRequest = objectMapper.convertValue(myPackage.getMessage(), LoginRequestDTO.class);

        Optional<EvoUser> optionalUser = evoUserService.findByLoginAndPass(
                loginRequest.getNickname(),
                loginRequest.getPassword()
        );

        if (optionalUser.isEmpty()) {
            sendAuthError(client);
            return;
        }

        Player player = authService.processLogin(client, optionalUser.get());
        messageSender.sendToClient(client, new MyPackage<>(player, EMessageType.AUTHORIZATION));
    }

    public void handleInitializationOfMainForm(MyPackage<?> myPackage, WebSocketSession session) {
        Client client = webSocketClientHolder.findAuthClientBySession(session);
        if (client == null) {
            logger.error("Client (" + session.getId() + " - session ID (not UUID)) failed to initialize");
            return;
        }

        client.setReadyToGetMessages(true);

        tableService.addPlayerNickName(playerRegistry.findPlayerByUUID(client.getPlayerUUID()));

        messageSender.broadcast(new MyPackage<>(webSocketClientHolder.getAuthenticatedClients().size(), EMessageType.CLIENT_COUNT));
        messageSender.broadcast(new MyPackage<>(tableService.getTable(), EMessageType.TABLE_STATUS));
    }

    private void sendAuthError(Client client) {
        logger.info("Client (" + client.getSession().getId() + " - session ID) failed authorization: Invalid credentials");
        messageSender.sendToClient(client, new MyPackage<>("Invalid login or password", EMessageType.AUTHORIZATION_ERROR));
    }
}