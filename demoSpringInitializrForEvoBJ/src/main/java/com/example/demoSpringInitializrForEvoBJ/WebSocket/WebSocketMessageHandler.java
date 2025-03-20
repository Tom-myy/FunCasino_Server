package com.example.demoSpringInitializrForEvoBJ.WebSocket;

import com.example.demoSpringInitializrForEvoBJ.Client;
import com.example.demoSpringInitializrForEvoBJ.ConnectionStatus;
import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.Game.*;
import com.example.demoSpringInitializrForEvoBJ.Game.Table.Seat;
import com.example.demoSpringInitializrForEvoBJ.Game.Table.Table;
import com.example.demoSpringInitializrForEvoBJ.GameToMessageHandlerListener;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.example.demoSpringInitializrForEvoBJ.myPackage.ClientFinder;
import com.example.demoSpringInitializrForEvoBJ.myPackage.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MessageProcessor;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.service.EvoUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

@Component
public class WebSocketMessageHandler extends TextWebSocketHandler implements GameToMessageHandlerListener, TimerObserver {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private static final int TIME_BEFORE_GAME = 15000;//seconds

    private List<Player> players = new LinkedList<>();//for money management

    public Player getPlayerByPlayerUUID(UUID playerUUID) {//TODO move it some better place and think over it better
        for (Player player : players) {
            if (player.getPlayerUUID().equals(playerUUID)) {
                return player;
            }
        }
        return null;
    }

    private boolean isPlayersChangingCompleted = false;
    private final EvoUserService evoUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Table table = new Table(players, this::playersBroadcast);
    private final MessageProcessor messageProcessor;
    private MyTimer timerForGameStart = new MyTimer(this);
    private final Game game = new Game(this, table, players, this::playersBroadcast, timerForGameStart);
    @Setter
    @Getter
    private boolean gameStarted = false;
    private ClientFinder clientFinder;
    private final Map<String, Client> temporaryClients = new ConcurrentHashMap<>();
    private final Map<UUID, Client> authenticatedClients = new ConcurrentHashMap<>();

    public WebSocketMessageHandler(EvoUserService evoUserService) {
        this.evoUserService = evoUserService;
        messageProcessor = new MessageProcessor(evoUserService);
        clientFinder = new ClientFinder(temporaryClients, authenticatedClients, players);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        temporaryClients.put(session.getId(), new Client(session));
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

        String tempClientUUID = clientFinder.findTempUUIDBySession(session);
        UUID authClientUUID = clientFinder.findAuthUUIDBySession(session);
        if (tempClientUUID == null && authClientUUID == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        Client tempClient = null;
        Client authClient = null;

        if (tempClientUUID != null)
            tempClient = clientFinder.findTempClientByUUID(tempClientUUID);

        if (authClientUUID != null)
            authClient = clientFinder.findAuthClientByUUID(authClientUUID);

        if (tempClient == null & authClient == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        if (authClient != null) {
            UUID clientUUID = clientFinder.findAuthUUIDBySession(session);
            logger.info("Received (" + myPackage.getMessageType() + ") message from client with UUID (" + clientUUID + ")");
        } else if (tempClient != null) {
            logger.info("Received (" + myPackage.getMessageType() + ") message from client (" + session.getId() + " - session ID (not UUID))");
        } else {
            logger.error("Oh piece of shit, smth went wrong!");
            return;
        }

        switch (myPackage.getMessageType()) {
            case EMessageType.AUTHORIZATION: {
                Client client = clientFinder.findTempClientBySession(session);

                if (client == null) {
                    logger.error("client == null in AUTHORIZATION");
                    return;
                }

                MyPackage<?> responsePackage = messageProcessor.handleAuthorization(myPackage);

                if (responsePackage.getMessageType().equals(EMessageType.AUTHORIZATION_ERROR)) {
                    logger.info("Client (" + client.getSession().getId() + " - session ID (not UUID)) failed authorization: Invalid credentials");
                    sendToClient(client, responsePackage);
                    return;
                }

                EvoUserDTO evoUserDTO = objectMapper.convertValue(responsePackage.getMessage(), EvoUserDTO.class);

                if (evoUserDTO == null) {
                    logger.error("evoUserDTO is null");
                    return;
                }

                boolean reconnectedPlayer = false;
                for (Player p : players) {
                    if (p.getPlayerUUID().equals(evoUserDTO.getPlayerUUID())) {
                        reconnectedPlayer = true;
                        break;
                    }
                }

                Player player = null;

                if (reconnectedPlayer) {
                    temporaryClients.remove(client.getSession().getId());
                    authenticatedClients.put(evoUserDTO.getPlayerUUID(), client);
                    client.setPlayerUUID(evoUserDTO.getPlayerUUID());
                    player = getPlayerByPlayerUUID(client.getPlayerUUID());
                    if (player == null) {
                        logger.error("player == null");
                        return;
                    }
                    logger.info(evoUserDTO.getNickName() + " (" + evoUserDTO.getPlayerUUID() + ") was reconnected successfully");
                    client.setConnectionStatusToConnect();
                    sendToClient(client, new MyPackage<>(player, EMessageType.AUTHORIZATION));

                } else {
                    temporaryClients.remove(client.getSession().getId());
                    authenticatedClients.put(evoUserDTO.getPlayerUUID(), client);

                    player = new Player();
                    player.setEvoUserDTO(evoUserDTO);

                    logger.info("Login successful for user " + evoUserDTO.getNickName() + " (" + evoUserDTO.getPlayerUUID() + ")");
                    sendToClient(client, new MyPackage<>(player, EMessageType.AUTHORIZATION));
                    players.add(player);
                    table.addPlayerNickName(player);
                }

                client.setPlayerUUID(player.getPlayerUUID());

                break;
            }

            case EMessageType.MAIN_FORM_INITIALIZATION: {
                UUID clientUUID = clientFinder.findAuthUUIDBySession(session);
                if (clientUUID == null) {
                    logger.error("Client (" + session.getId() + " - session ID (not UUID)) failed to initialize");
                    return;
                }

                Client client = clientFinder.findAuthClientByUUID(clientUUID);
                client.setConnectionStatusToConnect();
                authenticatedClients.put(clientUUID, client);

                client.setReadyToGetMessages(true);

                broadcast(new MyPackage<>(clientCount(), EMessageType.CLIENT_COUNT));

                broadcast(new MyPackage<>(table, EMessageType.TABLE_STATUS));

                return;
            }

            case EMessageType.TAKE_SEAT: {
                Seat seatForTaking = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

                //changing seat in Table
                if (table.isSeatBusy(seatForTaking.getSeatNumber())) {
                    logger.error("Seat is busy");
                    return;
                }

                table.addSeat(seatForTaking);

                //changing seat in Player
                for (Player p : players) {
                    if (p.getPlayerUUID().equals(seatForTaking.getPlayerUUID())) {
                        p.addSeat(seatForTaking);
                        sendToClient(seatForTaking.getPlayerUUID(), new MyPackage<>(p, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
                        break;
                    }
                }

                broadcast(new MyPackage<>(table.getSeats(), EMessageType.SEATS));

                if (table.isThereSeatWithBetForPlayer(seatForTaking.getPlayerUUID())) {//TODO think over it - it doesnt work properly
                    sendToClient(seatForTaking.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else
                    sendToClient(seatForTaking.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));

                break;
            }

            case EMessageType.LEAVE_SEAT: {
                Seat seatForLeaving = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

                if (!table.isSeatBusy(seatForLeaving.getSeatNumber())) {
                    logger.error("There is no such seat for leaving at the table");
                    new Exception().printStackTrace();
                    return;
                }

                {//changing bet for seat in Player
                    Player newPlayerForLeaving = null;
                    int tmpInd = -1;
                    for (Player p : players) {
                        if (p.getPlayerUUID().equals(seatForLeaving.getPlayerUUID())) {
                            newPlayerForLeaving = p;
                            for (Seat s : p.getSeats()) {
                                if (s.equalsBySeatNumberAndUUID(seatForLeaving)) {
                                    tmpInd = p.getSeats().indexOf(s);
                                }
                            }
                        }
                    }

                    if (tmpInd == -1) {
                        logger.error("tmpInd == -1, no one player doesn't have such seat for leaving");
                        return;
                    }
                    if (newPlayerForLeaving == null) {
                        logger.error("newPlayerForLeaving is empty");
                        return;
                    }

                    newPlayerForLeaving.getSeats().remove(tmpInd);
                    newPlayerForLeaving.changeBalance(seatForLeaving.getCurrentBet());

                    sendToClient(seatForLeaving.getPlayerUUID(), new MyPackage<>(newPlayerForLeaving, EMessageType.CURRENT_DATA_ABOUT_PLAYER));//TODO here was added ---
                }

                table.removePlayerAtTheTableByKey(seatForLeaving.getSeatNumber());

                if (table.isThereSeatWithBetForPlayer(seatForLeaving.getPlayerUUID())) {//TODO think over it - it doesnt work properly
                    sendToClient(seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else if (table.isThereSeatForPlayer(seatForLeaving.getPlayerUUID())) {
                    sendToClient(seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else
                    sendToClient( seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.EMPTY_TABLE, EMessageType.E_GAME_STATUS_FOR_INTERFACE));

                seatForLeaving = null;
                broadcast(new MyPackage<>(table.getSeats(), EMessageType.SEATS));//бродкастить коллекицю мест со статусом EMessageType.SEATS

                break;
            }

            case EMessageType.UPDATE_SEAT_BET: {
                Seat seatForBetUpdating = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

                {//changing bet for seat in Table
                    int ind_UPDATE_SEAT_BET = -1;
                    for (Seat seat : table.getSeats()) {
                        if (seat.equalsExcludingCurrentBet(seatForBetUpdating)) {
                            ind_UPDATE_SEAT_BET = table.getSeats().indexOf(seat);
                            break;
                        }
                    }

                    if (ind_UPDATE_SEAT_BET == -1) {
                        logger.error("Smth went wrong and ind_UPDATE_SEAT_BET == -1");
                        return;
                    }

                    table.getSeats().set(ind_UPDATE_SEAT_BET, seatForBetUpdating);
                }

                int seatIndexInPlayer = -1;
                {//changing bet for seat in Player
                    Player playerForBet = null;
                    for (Player p : players) {
                        if (p.getPlayerUUID().equals(seatForBetUpdating.getPlayerUUID())) {
                            playerForBet = p;
                            for (Seat s : p.getSeats()) {
                                if (s.equalsBySeatNumberAndUUID(seatForBetUpdating)) {
                                    seatIndexInPlayer = p.getSeats().indexOf(s);
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (seatIndexInPlayer == -1) {
                        logger.error("tmpInd == -1, no one player doesn't have such seat for bet updating");
                        return;
                    }
                    if (playerForBet == null) {
                        logger.error("newPlayerForBet is empty");
                        return;
                    }

                    Seat oldSeat = playerForBet.getSeats().get(seatIndexInPlayer);
                    BigDecimal oldBet = oldSeat.getCurrentBet();

                    playerForBet.getSeats().set(seatIndexInPlayer, seatForBetUpdating);
                    playerForBet.changeBalance((seatForBetUpdating.getCurrentBet().subtract(oldBet)).negate());

                    sendToClient(seatForBetUpdating.getPlayerUUID(), new MyPackage<>(playerForBet, EMessageType.CURRENT_DATA_ABOUT_PLAYER));//TODO here was added ---

                    {//before-timerForGameStart
                        if (!timerForGameStart.isRunning()) {
                            if (table.isThereGameSeat()) {
                                new Thread(() -> {
                                    timerForGameStart.startTimer(TIME_BEFORE_GAME, "BEFORE_GAME");
                                }).start();

                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }

                                new Thread(() -> {
                                    while (timerForGameStart.isRunning()) {}

                                    if (table.isThereGameSeat()) {
                                        smthAboutGame();
                                    }
                                }).start();
                            }
                        } /*else timerForGameStart.stopTimer();*///TODO doesnt work as i thought...
                    }
                }

                broadcast(new MyPackage<>(table.getSeats(), EMessageType.SEATS));
                sendToClient(seatForBetUpdating.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                break;
            }

            case EMessageType.REQUEST_TO_START_GAME: {
                if (table.isThereGameSeat()) {
                    UUID clientUUID = clientFinder.findAuthUUIDBySession(session);

                    if (clientUUID == null) {
                        logger.error("clientUUID == null for REQUEST_TO_START_GAME");
                        return;
                    }

                    for (Player p : players) {
                        if (clientUUID.equals(p.getPlayerUUID())) {
                            p.setWantsToStartGame(true);
                            break;
                        }
                    }

                    List<Player> tmpPlayersWithBet = new ArrayList<>();

                    for (Seat s : table.getAndSetGameSeats()) {
                        for (Player p : players) {
                            if (s.getPlayerUUID().equals(p.getPlayerUUID()) /*&& p.isWantsToStartGame()*/) {
                                tmpPlayersWithBet.add(p);
                                break;
                            }
                        }
                    }

                    boolean allPlayersWantsToStartGame = true;
                    for (Player p : tmpPlayersWithBet) {
                        if (!p.isWantsToStartGame()) {
                            allPlayersWantsToStartGame = false;
                            break;
                        }
                    }

                    if (allPlayersWantsToStartGame) {
                        smthAboutGame();
                    }
                } else logger.error("There is no game seat");
                break;
            }

            case EMessageType.GAME_DECISION: {//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
                if (!table.isGame()) {
                    logger.debug("GAME_DECISION was received, but game is not started");
                    return;
                }

                logger.debug("GAME_DECISION was received");
                EDecision decision = objectMapper.convertValue(myPackage.getMessage(), EDecision.class);
                game.setDecisionField(decision);
                logger.debug("GAME_DECISION was set");
                break;
            }

            default:
                logger.error("Server got a unknown MESSAGE type!");
        }
    }

    public void smthAboutGame(){
        new Thread(() -> {//TODO add if(game.isGame) if game is already playing
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<List<Player>> future = executor.submit(() -> game.startGame());

            List<Player> changedPlayersAfterGame = null; // блокирует до завершения
            try {
                changedPlayersAfterGame = future.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            executor.shutdown();

            if (changedPlayersAfterGame == null) {
                logger.error("changedPlayersAfterGame == null");
                return;
            }

            changedPlayersAfterGame = evoUserService.updateUsersAfterGame(changedPlayersAfterGame);

            if (changedPlayersAfterGame == null) {
                logger.error("changedPlayersAfterGame == null");
                return;
            }

            List<EvoUserDTO> freshDtos;
            freshDtos = evoUserService.getUpdatedUsers(changedPlayersAfterGame);

            if (freshDtos == null) {
                logger.error("changedPlayersAfterGame == null");
                return;
            }

            for(EvoUserDTO dto : freshDtos){
                sendToClient(dto.getPlayerUUID(), new MyPackage<>(dto, EMessageType.FRESH_DTOS));
            }
        }).start();
    }

    private int clientCount() {
        int count = 0;
        for (Client c : authenticatedClients.values()) {
            if (c.getConnectionStatus() == ConnectionStatus.CONNECTED) ++count;
        }
        return count;
    }

    public List<Player> getPlayersInGame() {
        List<Player> gamePlayers = new ArrayList<>();

        for (Player p : players) {
            if (p.isInTheGame()) {
                gamePlayers.add(p);
            }
        }

        System.out.println(gamePlayers.size());
        return gamePlayers;
    }

    public void playersBroadcast() {//It's for sending to certain player his player data
        for (Player player : players) {
            sendToClient(player.getPlayerUUID(), new MyPackage<>(player, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession client, CloseStatus status) {
        new Thread(() -> {
            removingInactiveClient(client);
        }).start();
    }

    public void removingInactiveClient(WebSocketSession session) {
        if (authenticatedClients.containsValue(clientFinder.findAuthClientBySession(session))) {
            Client client = clientFinder.findAuthClientBySession(session);
            client.setConnectionStatusToDisconnect();
            while (table.isGame()) {

            }

            if (client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                return;
            }
            UUID clientUUID = clientFinder.findAuthUUIDBySession(session);

            table.removePlayersSeatsAtTheTable(clientFinder.findPlayerByUUID(clientUUID));
            authenticatedClients.remove(clientFinder.findAuthUUIDBySession(session));
            players.remove(clientFinder.findPlayerByUUID(clientUUID));
            //сделать некий метод, который после отключения клиента будет проверять списки клиентов,
            //игроков и мест, и будет удалять отключившигося клиента оттуда

            logger.info("Client disconnected: " + clientUUID);

            broadcast(new MyPackage<>(clientCount(), EMessageType.CLIENT_COUNT));
            broadcast(new MyPackage<>(table, EMessageType.TABLE_STATUS));
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

        synchronized (authenticatedClients) {
            for (Client client : authenticatedClients.values()) {
                if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages())
                    continue;

                try {
                    client.getSession().sendMessage(new TextMessage(responseJson));
                    logger.info("Broadcast to (" + clientFinder.findAuthUUIDBySession(client.getSession()) + "): msg_json - " + responseJson);
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

    @Override
    public void sendToClient(UUID playerUUID, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Client client = clientFinder.findAuthClientByUUID(playerUUID);

        if (client == null) {
            logger.error("Client not found for UUID: " + playerUUID);
            return;
        }

        if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            client.getSession().sendMessage(new TextMessage(responseJson));
            logger.info("SendToClient (" + playerUUID + "): msg_json - " + responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
                //mb todo smth
            } else
                e.printStackTrace();
        }
    }

    public void sendToClient(Client client, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            client.getSession().sendMessage(new TextMessage(responseJson));
            logger.info("SendToClient (" + client.getSession().getId() + " - session ID (not UUID)): msg_json - " + responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {

            } else
                e.printStackTrace();
        }
    }

    @Override
    public void timeWasChanged(int seconds, String flag) {
        if (flag.equalsIgnoreCase("BEFORE_GAME")) {
            if (seconds == -1)
                broadcast(new MyPackage<>("", EMessageType.TIMER_CANCEL));//TODO mustn't broadcast - players at the table\in the game
            else
                broadcast(new MyPackage<>(seconds, EMessageType.TIMER));//TODO mustn't broadcast - players at the table\in the game
        } else if (flag.equalsIgnoreCase("GAME")) {
            for (Player p : getPlayersInGame()) {
                if (seconds == -1)
                    sendToClient(p.getPlayerUUID(), new MyPackage<>(seconds, EMessageType.TIMER_CANCEL));
                else
                    sendToClient(p.getPlayerUUID(), new MyPackage<>(seconds, EMessageType.TIMER));
            }
        }
    }
}