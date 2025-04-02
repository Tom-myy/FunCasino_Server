/*
package com.example.demoSpringInitializrForEvoBJ.myPackage;

import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.DTO.LoginRequestDTO;
import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.repository.EvoUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import static java.util.UUID.randomUUID;

public class MessageProcessor {
    private final EvoUserRepository evoUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageProcessor(EvoUserRepository evoUserRepository) {
        this.evoUserRepository = evoUserRepository;
    }

    public MyPackage handleAuthorization(MyPackage<?> myPackage, String clientUUID) {

        LoginRequestDTO loginRequest = objectMapper.convertValue(myPackage.getMessage(), LoginRequestDTO.class);

        // Взаимодействие с БД
        Optional<EvoUser> optionalUser = evoUserRepository.findByLoginAndPass(
                loginRequest.getNickname(),
                loginRequest.getPassword()
        );

        if (optionalUser.isPresent()) {
            EvoUser user = optionalUser.get();

            // Создание ответа
            EvoUserDTO responseUser = new EvoUserDTO(
//                    randomUUID().toString(),
                    clientUUID,
                    user.getName(),
                    user.getSurname(),
                    user.getNickName(),
                    user.getBalance()
            );

            // Отправка успешного ответа
            return new MyPackage<>(responseUser, EMessageType.AUTHORIZATION);
        } else {
            // Отправка ошибки
            return new MyPackage<>("Invalid login or password", EMessageType.AUTHORIZATION_ERROR);
        }
    }

    public MyPackage handleSeat(MyPackage<?> myPackage) {

        return new MyPackage<>("", EMessageType.TAKE_SEAT);

    }
}
*/
package com.example.demoSpringInitializrForEvoBJ.myPackage;

import com.example.demoSpringInitializrForEvoBJ.Client;
import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.DTO.LoginRequestDTO;
import com.example.demoSpringInitializrForEvoBJ.Message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.example.demoSpringInitializrForEvoBJ.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.TableService;
import com.example.demoSpringInitializrForEvoBJ.WebSocketClientHolder.WebSocketClientHolder;
import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.service.EvoUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final EvoUserService evoUserService;
    private final ObjectMapper objectMapper;
    private final MessageSender messageSender;
    private final TableService tableService;
    private final PlayerRegistry playerRegistry;
    private final WebSocketClientHolder webSocketClientHolder;

    public AuthHandler(EvoUserService evoUserService, ObjectMapper objectMapper, MessageSender messageSender, TableService tableService, PlayerRegistry playerRegistry, WebSocketClientHolder webSocketClientHolder) {
        this.evoUserService = evoUserService;
        this.objectMapper = objectMapper;
        this.messageSender = messageSender;
        this.tableService = tableService;
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

        if (optionalUser.isPresent()) {
            EvoUser user = optionalUser.get();

            EvoUserDTO responseUser = new EvoUserDTO(
                    user.getUserID(),
                    user.getName(),
                    user.getSurname(),
                    user.getNickName(),
                    user.getBalance()
            );

            boolean reconnectedPlayer = false;
            for (Player p : getPlayers()) {
                if (p.getPlayerUUID().equals(responseUser.getPlayerUUID())) {
                    reconnectedPlayer = true;
                    break;
                }
            }

            Player player = null;

            if (reconnectedPlayer) {
                getTemporaryClients().remove(client.getSession().getId());
                getAuthenticatedClients().put(responseUser.getPlayerUUID(), client);
                client.setPlayerUUID(responseUser.getPlayerUUID());
                player = playerRegistry.findPlayerByUUID(client.getPlayerUUID());
                if (player == null) {
                    logger.error("player == null");
                    return;
                }
                logger.info(responseUser.getNickName() + " (" + responseUser.getPlayerUUID() + ") was reconnected successfully");
                client.setConnectionStatusToConnect();
                messageSender.sendToClient(client, new MyPackage<>(player, EMessageType.AUTHORIZATION));

            } else {
                getTemporaryClients().remove(client.getSession().getId());
                getAuthenticatedClients().put(responseUser.getPlayerUUID(), client);

                player = new Player();
                player.setEvoUserDTO(responseUser);

                logger.info("Login successful for user " + responseUser.getNickName() + " (" + responseUser.getPlayerUUID() + ")");
                messageSender.sendToClient(client, new MyPackage<>(player, EMessageType.AUTHORIZATION));
                getPlayers().add(player);
//                table.addPlayerNickName(player);//TODO add them during taking seats...
            }

            client.setPlayerUUID(player.getPlayerUUID());

        } else {
                logger.info("Client (" + client.getSession().getId() + " - session ID (not UUID)) failed authorization: Invalid credentials");
                messageSender.sendToClient(client, new MyPackage<>("Invalid login or password", EMessageType.AUTHORIZATION_ERROR));
        }
    }

    public void handleInitializationOfMainForm(MyPackage<?> myPackage, WebSocketSession session) {
        UUID clientUUID = webSocketClientHolder.findAuthUUIDBySession(session);
        if (clientUUID == null) {
            logger.error("Client (" + session.getId() + " - session ID (not UUID)) failed to initialize");
            return;
        }

        Client client = webSocketClientHolder.findAuthClientByUUID(clientUUID);
        client.setConnectionStatusToConnect();
        getAuthenticatedClients().put(clientUUID, client);

        client.setReadyToGetMessages(true);

        messageSender.broadcast(new MyPackage<>(getAuthenticatedClients().size(), EMessageType.CLIENT_COUNT));
        messageSender.broadcast(new MyPackage<>(tableService.getTable(), EMessageType.TABLE_STATUS));
    }
}
