package com.example.demoSpringInitializrForEvoBJ.websocket.handler;

import com.example.demoSpringInitializrForEvoBJ.common.GameExceptionProcessor;
import com.example.demoSpringInitializrForEvoBJ.common.GameSystemException;
import com.example.demoSpringInitializrForEvoBJ.game.model.Seat;
import com.example.demoSpringInitializrForEvoBJ.game.model.Table;
import com.example.demoSpringInitializrForEvoBJ.game.service.GameService;
import com.example.demoSpringInitializrForEvoBJ.game.service.TableService;
import com.example.demoSpringInitializrForEvoBJ.player.service.PlayerService;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.websocket.WebSocketClientHolder;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.UUID;

@Component
public class SeatHandler {
    //TODO create validation methode for seat checking...
    //TODO add logging with info (etc.) level for successful actions
    private static final Logger logger = LoggerFactory.getLogger(SeatHandler.class);
    private final ObjectMapper objectMapper;//TODO it seems I need check for null before use
    private final MessageSender messageSender;
    private final TableService tableService;
    private final PlayerService playerService;
    private final GameService gameService;
    private final GameExceptionProcessor exceptionProcessor;//TODO check for null before use
    private final WebSocketClientHolder webSocketClientHolder;

    public SeatHandler(ObjectMapper objectMapper, MessageSender messageSender, TableService tableService, PlayerService playerService, GameService gameService, GameExceptionProcessor exceptionProcessor, WebSocketClientHolder webSocketClientHolder) {
        this.objectMapper = objectMapper;
        this.messageSender = messageSender;
        this.tableService = tableService;
        this.playerService = playerService;
        this.gameService = gameService;
        this.exceptionProcessor = exceptionProcessor;
        this.webSocketClientHolder = webSocketClientHolder;
    }

    /// copy past part, but it's like shortcut
    public Table getTable() {
        return tableService.getTable();
    }

    public void handleTakeSeat(MyPackage<?> myPackage, WebSocketSession session) {
        Seat seatForTaking = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

        try {
            tableService.addSeat(seatForTaking);
            playerService.addSeat(seatForTaking);
        } catch (GameSystemException e) {
            UUID playerUUID = webSocketClientHolder.findAuthUUIDBySession(session);
            exceptionProcessor.process(e, playerUUID);
            return;
        }

        messageSender.broadcast(new MyPackage<>(getTable().getSeats(), EMessageType.SEATS));

        tableService.sendPhaseUpdateToPlayer(seatForTaking);
    }

    public void handleLeaveSeat(MyPackage<?> myPackage, WebSocketSession session) {
        Seat seatForLeaving = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

        if (!tableService.isSeatBusy(seatForLeaving)) {
            logger.error("There is no such seat for leaving at the table");
            messageSender.sendToClient(
                    webSocketClientHolder.findAuthClientBySession(session),
                    new MyPackage<>("There is no such seat for leaving at the table.", EMessageType.LEAVE_SEAT_ERROR));
            return;
        }

        try {
            Player player = playerService.removeSeatAndRefund(seatForLeaving);
            tableService.removeSeat(seatForLeaving);
            messageSender.sendToClient(seatForLeaving.getPlayerUUID(), new MyPackage<>(player, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
        } catch (GameSystemException e) {
            exceptionProcessor.process(e, webSocketClientHolder.findAuthUUIDBySession(session));
            return;
        }

        tableService.sendPhaseUpdateToPlayer(seatForLeaving);

        messageSender.broadcast(new MyPackage<>(tableService.getSeats(), EMessageType.SEATS));//бродкастить коллекицю мест со статусом EMessageType.SEATS
    }

    public void handleUpdateSeatBet(MyPackage<?> myPackage, WebSocketSession session) {
        Seat seatForBetUpdating = objectMapper.convertValue(myPackage.getMessage(), Seat.class);

        //TODO check that in the received seat only the bet has been changed and nothing more
        //TODO check if there is such seat number in collection
        //TODO check if this seat belongs to correct client

        try {
            tableService.replaceSeatAndUpdateBetAtTheTable(seatForBetUpdating);
            Player player = playerService.replaceSeatAndUpdateBetInPlayer(seatForBetUpdating);
            messageSender.sendToClient(seatForBetUpdating.getPlayerUUID(), new MyPackage<>(player, EMessageType.CURRENT_DATA_ABOUT_PLAYER));//TODO here was added ---
        } catch (GameSystemException e) {
            exceptionProcessor.process(e, webSocketClientHolder.findAuthUUIDBySession(session));
            return;
        }

        gameService.tryStartBettingTime();

        messageSender.broadcast(new MyPackage<>(getTable().getSeats(), EMessageType.SEATS));

        tableService.sendPhaseUpdateToPlayer(seatForBetUpdating);
    }
}
