package com.example.demoSpringInitializrForEvoBJ.WebSocket;

import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.Game.EDecision;
import com.example.demoSpringInitializrForEvoBJ.Game.EGamePhaseForInterface;
import com.example.demoSpringInitializrForEvoBJ.Game.Game;
import com.example.demoSpringInitializrForEvoBJ.Game.MyTimer;
import com.example.demoSpringInitializrForEvoBJ.Game.Table.Seat;
import com.example.demoSpringInitializrForEvoBJ.Game.Table.Table;
import com.example.demoSpringInitializrForEvoBJ.GameToMessageHandlerListener;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.example.demoSpringInitializrForEvoBJ.myPackage.ClientFinder;
import com.example.demoSpringInitializrForEvoBJ.myPackage.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MessageProcessor;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.repository.EvoUserRepository;
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
public class WebSocketMessageHandler extends TextWebSocketHandler implements GameToMessageHandlerListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private static final int TIME_BEFORE_GAME = 30;//seconds

    private List<Player> players = new LinkedList<>();//for money management
    private boolean isPlayersChangingCompleted = false;
    private final EvoUserRepository evoUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Table table = new Table(players, this::playersBroadcast);
    private final MessageProcessor messageProcessor;
    private final Game game = new Game(this, table, players, this::playersBroadcast);
    private ClientFinder clientFinder;
    // Коллекция для хранения подключённых клиентов
    //    private final Set<WebSocketSession> clients = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, WebSocketSession> clients = Collections.synchronizedMap(new HashMap<>());
//    private final Map<String, WebSocketSession> clientsForSending = new HashMap<>();

    private MyTimer timerForGameStart = new MyTimer();

    public WebSocketMessageHandler(EvoUserRepository evoUserRepository) {
        this.evoUserRepository = evoUserRepository;
        messageProcessor = new MessageProcessor(evoUserRepository);
        clientFinder = new ClientFinder(clients);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Добавляем нового клиента в коллекцию
//        clients.put(generateUUID(), session);//TODO this one
//        clients.add(session);
//        System.out.println("Client connected: " + session.getId());
//        broadcast(new MyPackage<>(clients.size(), EMessageType.CLIENT_COUNT));
    }

    @Override
    public void handleTextMessage(WebSocketSession client, TextMessage message) {
        // Десериализация JSON в MyPackage
        String payload = message.getPayload();
        MyPackage<?> myPackage;
        try {
            myPackage = objectMapper.readValue(payload, new TypeReference<MyPackage<?>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Server got a unknown PACKAGE type!", e);
            return;
        }

        if (clients.containsValue(client)) {
            String clientUUID = clientFinder.findUUIDByClient(client);
            logger.info("Received (" + myPackage.getMessageType() + ") message from client with UUID (" + clientUUID + ")");
        } else
            logger.info("Received (" + myPackage.getMessageType() + ") message from client (" + client.getId() + " - session ID (not UUID))");

/*        String playerUUID = clientFinder.findUUIDByClient(client); //TODO this one
        if(playerUUID == null) {
            System.err.println("Smth went wrong, client wasn't found (line 58 =-)");
            return;
        }*/

/*       // Десериализация JSON в MyPackage
        String payload = message.getPayload();
        MyPackage<?> myPackage;
        try {
            myPackage = objectMapper.readValue(payload, new TypeReference<MyPackage<?>>() {});
        } catch (JsonProcessingException e) {
            System.err.println("Server got a unknown PACKAGE type!");
            throw new RuntimeException(e);
        }*/

        switch (myPackage.getMessageType()) {
            case EMessageType.AUTHORIZATION: {
                MyPackage<?> responsePackage = messageProcessor.handleAuthorization(myPackage);

                if (responsePackage.getMessageType().equals(EMessageType.AUTHORIZATION_ERROR)) {
                    logger.info("Client (" + client.getId() + " - session ID (not UUID)) failed authorization: Invalid credentials");
                    sendToClient(client, responsePackage);
                    return;
                }

                EvoUserDTO evoUserDTO = objectMapper.convertValue(responsePackage.getMessage(), EvoUserDTO.class);

                if (evoUserDTO == null) {
                    logger.error("evoUserDTO is null");
                    return;
                }

                logger.info("Login successful for user " + evoUserDTO.getNickName() + " (" + evoUserDTO.getPlayerUUID() + ")");

                clients.put(evoUserDTO.getPlayerUUID(), client);

                sendToClient(evoUserDTO.getPlayerUUID(), responsePackage);

                break;
            }

            case EMessageType.PLAYER: {

                Player player = null;
                player = objectMapper.convertValue(myPackage.getMessage(), Player.class);

                if (player == null) {
                    logger.error("Player is null!");
                    return;
                }

                players.add(player);
                table.addPlayerNickName(player);

                break;
            }

            case EMessageType.MAIN_FORM_INITIALIZATION: {
                broadcast(new MyPackage<>(clients.size(), EMessageType.CLIENT_COUNT));

//                String uuid = clients.get
                String clientUUID = "";
                for (Map.Entry<String, WebSocketSession> entry : clients.entrySet()) {
                    if (Objects.equals(entry.getValue(), client)) {
                        clientUUID = entry.getKey();
                    }
                }

                if (clientUUID.isEmpty()) {
                    logger.error("clientUUID is empty!");
                    return;
                }


//                sendToClient(clientUUID, new MyPackage<>(table, EMessageType.TABLE_STATUS));//тут лучше отправлять не коллекц мест, а стол со стутусом tableStatus
                broadcast(new MyPackage<>(table, EMessageType.TABLE_STATUS));//тут лучше отправлять не коллекц мест, а стол со стутусом tableStatus

                return;
            }

            case EMessageType.TAKE_SEAT: {
                Seat seatForTaking = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

                //changing seat in Table
                if (table.isSeatBusy(seatForTaking.getSeatNumber())) {
                    logger.error("Seat is busy");
//                    sendToClient(client, new MyPackage<>("", EMessageType.SEAT_BUSY));
                    return;
                }

                table.addSeat(seatForTaking);

                //changing seat in Player
                for (Player p : players) {
                    if (p.getPlayerUUID().equals(seatForTaking.getPlayerUUID())) {
                        p.addSeat(seatForTaking);
                        sendToClient(seatForTaking.getPlayerUUID(), new MyPackage<>(p, EMessageType.CURRENT_DATA_ABOUT_PLAYER));//TODO here was added ---
                        break;
                    }
                }
//                responsePackage = messageProcessor.handleSeat(myPackage);
//                responsePackage = new MyPackage<>("", EMessageType.SEAT_OK);//TODO broadcast for displaying to everyone
//                sendToClient(/*playerUUID*/ seatForTaking.getPlayerUUID(), new MyPackage<>(seatForTaking, EMessageType.TAKE_SEAT_OK));//мб не бродкастить это, а только там где TABLE _STATUS


//                broadcast(new MyPackage<>(table.getSeats(), EMessageType.TABLE_STATUS));//бродкастить коллекицю мест со статусом EMessageType.SEATS
                broadcast(new MyPackage<>(table.getSeats(), EMessageType.SEATS));//бродкастить коллекицю мест со статусом EMessageType.SEATS

                if (table.isThereSeatWithBetForPlayer(seatForTaking.getPlayerUUID())) {//TODO think over it - it doesnt work properly
                    sendToClient(/*playerUUID*/ seatForTaking.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else
                    sendToClient(/*playerUUID*/ seatForTaking.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));

//                sendToClient(/*playerUUID*/ seatForTaking.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));

                break;
            }

            case EMessageType.LEAVE_SEAT: {
                Seat seatForLeaving = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

                if (!table.isSeatBusy(seatForLeaving.getSeatNumber())) {
                    logger.error("There is no such seat for leaving at the table");
                    new Exception().printStackTrace();
//                    sendToClient(client, new MyPackage<>("", EMessageType.SEAT_BUSY));
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

/*                if (table.isThereSeatWithBetForPlayer(seatForLeaving.getPlayerUUID()) ||
                        table.isThereSeatForPlayer(seatForLeaving.getPlayerUUID())) {//TODO think over it - it doesnt work properly
                    sendToClient(*//*playerUUID*//* seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else
                    sendToClient(*//*playerUUID*//* seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.EMPTY_TABLE, EMessageType.E_GAME_STATUS_FOR_INTERFACE));*/
                if (table.isThereSeatWithBetForPlayer(seatForLeaving.getPlayerUUID())) {//TODO think over it - it doesnt work properly
                    sendToClient(/*playerUUID*/ seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else if (table.isThereSeatForPlayer(seatForLeaving.getPlayerUUID())) {
                    sendToClient(/*playerUUID*/ seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
                } else
                    sendToClient(/*playerUUID*/ seatForLeaving.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.EMPTY_TABLE, EMessageType.E_GAME_STATUS_FOR_INTERFACE));


//                seatForLeaving.setPlayerUUID(null);
                seatForLeaving = null;
                broadcast(new MyPackage<>(table.getSeats(), EMessageType.SEATS));//бродкастить коллекицю мест со статусом EMessageType.SEATS
//                broadcast(new MyPackage<>(table.getSeats(), EMessageType.TABLE_STATUS));//бродкастить коллекицю мест со статусом EMessageType.SEATS
//                responsePackage = messageProcessor.handleSeat(myPackage);
//                responsePackage = new MyPackage<>("", EMessageType.SEAT_OK);//TODO broadcast for displaying to everyone
//                sendToClient(playerUUID, new MyPackage<>(seatForLeaving, EMessageType.LEAVE_SEAT_OK));//мб не бродкастить это, а только там где TABLE _STATUS


                break;
            }

            case EMessageType.UPDATE_SEAT_BET: {
                Seat seatForBetUpdating = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

                {//changing bet for seat in Table
                    int ind_UPDATE_SEAT_BET = -1;
                    for (Seat seat : table.getSeats()) {
                        if (seat.equalsExcludingCurrentBet(seatForBetUpdating))
                            ind_UPDATE_SEAT_BET = table.getSeats().indexOf(seat);
                    }

                    if (ind_UPDATE_SEAT_BET == -1) {
                        logger.error("Smth went wrong and ind_UPDATE_SEAT_BET == -1");
                        return;
                    }

                    table.getSeats().set(ind_UPDATE_SEAT_BET, seatForBetUpdating);
                }

                {//changing bet for seat in Player
                    Player newPlayerForBet = null;
                    int tmpInd = -1;
                    for (Player p : players) {
                        if (p.getPlayerUUID().equals(seatForBetUpdating.getPlayerUUID())) {
                            newPlayerForBet = p;
                            for (Seat s : p.getSeats()) {
                                if (s.equalsBySeatNumberAndUUID(seatForBetUpdating)) {
                                    tmpInd = p.getSeats().indexOf(s);
                                }
                            }
                        }
                    }

                    if (tmpInd == -1) {
                        logger.error("tmpInd == -1, no one player doesn't have such seat for bet updating");
                        return;
                    }
                    if (newPlayerForBet == null) {
                        logger.error("newPlayerForBet is empty");
                        return;
                    }

                    newPlayerForBet.getSeats().set(tmpInd, seatForBetUpdating);
                    newPlayerForBet.changeBalance(-seatForBetUpdating.getCurrentBet());

                    sendToClient(seatForBetUpdating.getPlayerUUID(), new MyPackage<>(newPlayerForBet, EMessageType.CURRENT_DATA_ABOUT_PLAYER));//TODO here was added ---

                    {//before-timerForGameStart
                        if (!timerForGameStart.isRunning()) {
                            if (table.isThereGameSeat()) {
                                broadcast(new MyPackage<>(TIME_BEFORE_GAME, EMessageType.TIME_BEFORE_GAME));
                                new Thread(() -> {
                                    broadcast(new MyPackage<>("", EMessageType.START_TIMER_BEFORE_GAME));
                                    timerForGameStart.startTimer(TIME_BEFORE_GAME);
                                }).start();

                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }

                                new Thread(() -> {
                                    while (timerForGameStart.isRunning()) {
                                    }
                                    if (table.isThereGameSeat()) {
                                        new Thread(() -> {
                                            game.startGame();
                                        }).start();
                                    }
                                }).start();
                            }
                        }
                    }
                }

                broadcast(new MyPackage<>(table.getSeats(), EMessageType.SEATS));//бродкастить коллекицю мест со статусом EMessageType.SEATS
//                broadcast(new MyPackage<>(table.getSeats(), EMessageType.TABLE_STATUS));//бродкастить коллекицю мест со статусом EMessageType.SEATS
                sendToClient(seatForBetUpdating.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
//                sendToClient(client, new MyPackage<>(seatForBetUpdating, EMessageType.UPDATE_SEAT_BET_OK));//мб не бродкастить это, а только там где TABLE _STATUS
                break;
            }

/*            case EMessageType.REQUEST_TO_START_GAME: {
                if (table.isThereGameSeat())
                    new Thread(() -> {
                        game.startGame();
                    }).start();

                else logger.error("There is no game seat");
                break;
            }*/

            case EMessageType.REQUEST_TO_START_GAME: {
                if (table.isThereGameSeat()) {
                    String clientUUID = clientFinder.findUUIDByClient(client);

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
                        new Thread(() -> {
                            game.startGame();
                        }).start();
                    }
                } else logger.error("There is no game seat");
                break;
            }

            case EMessageType.GAME_DECISION: {//TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
                logger.debug("GAME_DECISION was received");
                EDecision decision = objectMapper.convertValue(myPackage.getMessage(), EDecision.class);
                game.setDecisionField(decision);
                logger.debug("GAME_DECISION was set");
                break;
            }

            default:
                logger.error("Server got a unknown MESSAGE type!");
        }

//        sendToClient(client, responsePackage);
    }

    public void playersBroadcast() {//It's for sending to certain player his player data
        for (Player player : players) {
            sendToClient(player.getPlayerUUID(), new MyPackage<>(player, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
        }
    }

/*    private void handleAuthorization(WebSocketSession session, MyPackage<?> myPackage) throws Exception {

        LoginRequestDTO loginRequest = objectMapper.convertValue(myPackage.getMessage(), LoginRequestDTO.class);

        // Взаимодействие с БД
        Optional<EvoUser> optionalUser = evoUserRepository.findByLoginAndPass(
                loginRequest.getNickname(),
                loginRequest.getPassword()
        );

        if (optionalUser.isPresent()) {
            EvoUser user = optionalUser.get();
            System.out.println("Login successful for user: " + user.getNickName());

            // Создание ответа
            EvoUserDTO responseUser = new EvoUserDTO(
                    generateUUID(),
                    user.getName(),
                    user.getSurname(),
                    user.getNickName(),
                    user.getBalance()
            );

            // Отправка успешного ответа
            sendToClient(session, new MyPackage<>(responseUser, EMessageType.AUTHORIZATION));
        } else {
            System.out.println("Login failed: Invalid credentials");
            // Отправка ошибки
            sendToClient(session, new MyPackage<>("Invalid login or password", EMessageType.AUTHORIZATION_ERROR));
        }
    }*/

    private String generateUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession client, CloseStatus status) {
        new Thread(() -> {
            removingInactiveClient(client);
        }).start();


    }

    public void removingInactiveClient(WebSocketSession client) {
        if (clients.containsValue(client)) {
            while(table.isGame()) {

            }

            String clientUUID = clientFinder.findUUIDByClient(client);

            clients.remove(clientFinder.findUUIDByClient(client));
            //сделать некий метод, который после отключения клиента будет проверять списки клиентов,
            //игроков и мест, и будет удалять отключившигося клиента оттуда

            logger.info("Client disconnected: " + clientUUID);

            broadcast(new MyPackage<>(clients.size(), EMessageType.CLIENT_COUNT));
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

        synchronized (clients) {
            for (WebSocketSession client : clients.values()) {
                try {
                    client.sendMessage(new TextMessage(responseJson));
                    logger.info("Broadcast to (" + clientFinder.findUUIDByClient(client) + "): msg_json - " + responseJson);
                } catch (Exception e) {
                    logger.error("Error while broadcasting: " + responseJson, e);
                    return;
                }
            }
        }
    }


    @Override
    public void sendToClient(/*WebSocketSession client*/ String playerUUID, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        WebSocketSession client = clients.get(playerUUID);

        try {
            client.sendMessage(new TextMessage(responseJson));
            logger.info("SendToClient (" + playerUUID + "): msg_json - " + responseJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToClient(WebSocketSession client, MyPackage<?> message) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            client.sendMessage(new TextMessage(responseJson));
//            logger.info("SendToClient (" + clientFinder.findUUIDByClient(client) + "):\n msg_json - " + responseJson);
            logger.info("SendToClient (" + client.getId() + " - session ID (not UUID)): msg_json - " + responseJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(MyPackage<?> myPackage) {

    }
}
