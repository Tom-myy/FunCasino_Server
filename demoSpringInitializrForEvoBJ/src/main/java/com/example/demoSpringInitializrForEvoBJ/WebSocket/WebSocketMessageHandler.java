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
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketMessageHandler extends TextWebSocketHandler implements GameToMessageHandlerListener, TimerObserver {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    private static final int TIME_BEFORE_GAME = 15;//seconds

    private List<Player> players = new LinkedList<>();//for money management
    public Player getPlayerByPlayerUUID(String playerUUID) {//TODO move it some better place and think over it better
        for (Player player : players) {
            if (player.getPlayerUUID().equals(playerUUID)) {
                return player;
            }
        }
        return null;
    }
    private boolean isPlayersChangingCompleted = false;
    private final EvoUserRepository evoUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Table table = new Table(players, this::playersBroadcast);
    private final MessageProcessor messageProcessor;
    private MyTimer timerForGameStart = new MyTimer(/*TIME_BEFORE_GAME,*/ this);
    private final Game game = new Game(this, table, players, this::playersBroadcast, timerForGameStart);
    private ClientFinder clientFinder;
    // Коллекция для хранения подключённых клиентов
    //    private final Set<WebSocketSession> clients = Collections.synchronizedSet(new HashSet<>());
//    private final Map<String, Client> notReadyClients = Collections.synchronizedMap(new HashMap<>());
//    private final Map<String, Client> clients = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    ;
//    private final Map<String, WebSocketSession> clientsForSending = new HashMap<>();


    public WebSocketMessageHandler(EvoUserRepository evoUserRepository) {
        this.evoUserRepository = evoUserRepository;
        messageProcessor = new MessageProcessor(evoUserRepository);
        clientFinder = new ClientFinder(/*notReadyClients,*/ clients, players);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Добавляем нового клиента в коллекцию
        clients.put(/*generateUUID()*/session.getId(), new Client(session));
//        clients.add(session);
//        System.out.println("Client connected: " + session.getId());
//        broadcast(new MyPackage<>(clients.size(), EMessageType.CLIENT_COUNT));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
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

/*        String tmpClientUUID = clientFinder.findNotReadyUUIDBySession(session);
        if(tmpClientUUID == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        Client tmpClient = clientFinder.findReadyClientByUUID(tmpClientUUID);
        if(tmpClient == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        if (clients.containsValue(tmpClient)) {
//            String clientUUID = clientFinder.findReadyUUIDBySession(client);
            logger.info("Received (" + myPackage.getMessageType() + ") message from client with UUID (" + tmpClientUUID + ")");
        } else
            logger.info("Received (" + myPackage.getMessageType() + ") message from client (" + tmpClient.getSession().getId() + " - session ID (not UUID))");*/

//        if (clients.containsValue(client)) {

        String tmpClientUUID = clientFinder.findUUIDBySession(session);
        if (tmpClientUUID == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

        Client tmpClient = clientFinder.findClientByUUID(tmpClientUUID);
        if (tmpClient == null) {
            logger.error("Server got a client with unknown UUID, msg type - " + myPackage.getMessageType());
            return;
        }

//        if (clients.containsValue(client)) {
        if (tmpClient.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            String clientUUID = clientFinder.findUUIDBySession(session);
            logger.info("Received (" + myPackage.getMessageType() + ") message from client with UUID (" + clientUUID + ")");
        } else
            logger.info("Received (" + myPackage.getMessageType() + ") message from client (" + session.getId() + " - session ID (not UUID))");

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
//                Client client = new Client(session);
                Client client = clientFinder.findClientBySession(session);
//                String clientUUID = clientFinder.findUUIDBySession(session);

                if (client == null /*|| clientUUID == null*/) {
//                    logger.error("client == null || clientUUID == null in AUTHORIZATION");
                    logger.error("client == null in AUTHORIZATION");
                    return;
                }

                MyPackage<?> responsePackage = messageProcessor.handleAuthorization(myPackage/*, clientUUID*/);

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
                    clients.remove(client.getSession().getId());
                    clients.put(evoUserDTO.getPlayerUUID(), client);
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
                    clients.remove(client.getSession().getId());
                    clients.put(evoUserDTO.getPlayerUUID(), client);

                    player = new Player();//new
                    player.setEvoUserDTO(evoUserDTO);//new

                    logger.info("Login successful for user " + evoUserDTO.getNickName() + " (" + evoUserDTO.getPlayerUUID() + ")");
//                    sendToClient(client, responsePackage);
                    sendToClient(client, new MyPackage<>(player, EMessageType.AUTHORIZATION));
                    players.add(player);
                    table.addPlayerNickName(player);
                }




//                Client client = clientFinder.findClientByUUID(player.getPlayerUUID());
//                if (client == null) {
//                    logger.error("Smth went wrong in PLAYER");
//                    return;
//                }
                client.setPlayerUUID(player.getPlayerUUID());

//                clients.put(evoUserDTO.getPlayerUUID(), client);
//                notReadyClients.put(evoUserDTO.getPlayerUUID(), client);

//                sendToClient(evoUserDTO.getPlayerUUID(), responsePackage);

                break;
            }

/*            case EMessageType.PLAYER: {

                Player player = null;
                player = objectMapper.convertValue(myPackage.getMessage(), Player.class);

                if (player == null) {
                    logger.error("Player is null!");
                    return;
                }

                players.add(player);
                table.addPlayerNickName(player);

                Client client = clientFinder.findClientByUUID(player.getPlayerUUID());
                if (client == null) {
                    logger.error("Smth went wrong in PLAYER");
                    return;
                }
                client.setPlayerUUID(player.getPlayerUUID());

                break;
            }*/

            case EMessageType.MAIN_FORM_INITIALIZATION: {
                String clientUUID = clientFinder.findUUIDBySession(session);
                if (clientUUID == null) {
                    logger.error("Client (" + session.getId() + " - session ID (not UUID)) failed to initialize");
                    return;
                }

//                Client client = notReadyClients.get(clientUUID);
                Client client = clientFinder.findClientByUUID(clientUUID);
                client.setConnectionStatusToConnect();
                clients.put(clientUUID, client);

                client.setReadyToGetMessages(true);

                broadcast(new MyPackage<>(/*clients.size()*/clientCount(), EMessageType.CLIENT_COUNT));

//                String uuid = clients.get
/*                String clientUUID = "";
                for (Map.Entry<String, WebSocketSession> entry : clients.entrySet()) {
                    if (Objects.equals(entry.getValue(), client)) {
                        clientUUID = entry.getKey();
                    }
                }

                if (clientUUID.isEmpty()) {
                    logger.error("clientUUID is empty!");
                    return;
                }*/


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
//                                broadcast(new MyPackage<>(TIME_BEFORE_GAME, EMessageType.TIME_BEFORE_GAME));
                                new Thread(() -> {
                                    timerForGameStart.startTimer(TIME_BEFORE_GAME, "BEFORE_GAME");
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
//                                            timerForGameStart.stopTimer();
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
                    String clientUUID = clientFinder.findUUIDBySession(session);

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
                        new Thread(() -> {//TODO add if(game.isGame) if game is already playing
                            game.startGame();
                        }).start();
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

//        sendToClient(client, responsePackage);
    }

    private int clientCount() {
        int count = 0;
        for (Client c : clients.values()) {
            if (c.getConnectionStatus() == ConnectionStatus.CONNECTED) ++count;
        }
        return count;
    }

/*    public List<Player> getPlayersInGame() {
        List<Player> gamePlayers = new ArrayList<>();

        for (Player p : players) {
            if (p.isWantsToStartGame()) {
                gamePlayers.add(p);
            }
        }

        System.out.println(gamePlayers.size());
        return gamePlayers;
    }*/

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

    public void removingInactiveClient(WebSocketSession session) {

        if (clients.containsValue(clientFinder.findClientBySession(session))) {
            Client client = clientFinder.findClientBySession(session);
            client.setConnectionStatusToDisconnect();
            while (table.isGame()) {

            }

            if(client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                return;
            }
            String clientUUID = clientFinder.findUUIDBySession(session);

            table.removePlayersSeatsAtTheTable(clientFinder.findPlayerByUUID(clientUUID));
            clients.remove(clientFinder.findUUIDBySession(session));
            players.remove(clientFinder.findPlayerByUUID(clientUUID));
            //сделать некий метод, который после отключения клиента будет проверять списки клиентов,
            //игроков и мест, и будет удалять отключившигося клиента оттуда

            logger.info("Client disconnected: " + clientUUID);

            broadcast(new MyPackage<>(/*clients.size()*/clientCount(), EMessageType.CLIENT_COUNT));
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

        synchronized (clients) {
            for (Client client : clients.values()) {
                if(message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) continue;

                try {
                    client.getSession().sendMessage(new TextMessage(responseJson));
                    logger.info("Broadcast to (" + clientFinder.findUUIDBySession(client.getSession()) + "): msg_json - " + responseJson);
                } catch (Exception e) {
                    if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {

                    } else {
                        e.printStackTrace();
                        logger.error("Error while broadcasting: " + responseJson, e);
//                    return;
                    }
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

        Client client = clientFinder.findClientByUUID(playerUUID);

        if (client == null) {
            logger.error("Client not found for UUID: " + playerUUID);
            return;
        }

//        WebSocketSession session = clients.get(playerUUID);

        if(message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            client.getSession().sendMessage(new TextMessage(responseJson));
            logger.info("SendToClient (" + playerUUID + "): msg_json - " + responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {

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

        if(message.getMessageType() == EMessageType.CHANGED_SEAT_FOR_GAME && !client.isReadyToGetMessages()) return;

        try {
            client.getSession().sendMessage(new TextMessage(responseJson));
//            logger.info("SendToClient (" + clientFinder.findUUIDByClient(client) + "):\n msg_json - " + responseJson);
            logger.info("SendToClient (" + client.getSession().getId() + " - session ID (not UUID)): msg_json - " + responseJson);
        } catch (Exception e) {
            if (client.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {

            } else
                e.printStackTrace();
        }
    }

    @Override
    public void onMessage(MyPackage<?> myPackage) {

    }

    @Override
    public void timeWasChanged(int seconds, String flag) {
/*        System.out.println("in timeWasChanged");
        for (Player p : getPlayersInGame()) {
            System.out.println("before time sending");
            sendToClient(p.getPlayerUUID(), new MyPackage<>(seconds, EMessageType.TIMER));
            System.out.println("time must be sent");
        }*/
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
