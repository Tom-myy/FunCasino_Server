package com.example.demoSpringInitializrForEvoBJ.game.service;

import com.example.demoSpringInitializrForEvoBJ.game.EGamePhaseForInterface;
import com.example.demoSpringInitializrForEvoBJ.game.model.Seat;
import com.example.demoSpringInitializrForEvoBJ.game.model.Table;
import com.example.demoSpringInitializrForEvoBJ.common.GameSystemException;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Component
public class TableService {
    private static final Logger logger = LoggerFactory.getLogger(TableService.class);

    private final Table table;
    private final PlayerRegistry playerRegistry;
    private final MessageSender messageSender;

    public TableService(MessageSender messageSender, PlayerRegistry playerRegistry) {
        this.messageSender = messageSender;
        this.playerRegistry = playerRegistry;
        this.table = new Table(playerRegistry.getPlayers());
    }

    public boolean isTableReadyToStartGame() {
        List<Seat> gameSeats = new ArrayList<>();

        for (Seat seat : table.getSeats()) {
            if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0)
                gameSeats.add(seat);
        }

        return !gameSeats.isEmpty();
    }

    public void addSeat(Seat seat) throws GameSystemException {
        if (isSeatBusy(seat)) {
            throw new GameSystemException("Seat is already taken");
        }
        table.addSeat(seat);
    }

    public List<Seat> getSeats() {
        return table.getSeats();
    }
    public List<Player> getPlayers() {
        return table.getPlayers();
    }

    public void removeSeat(Seat seat) throws GameSystemException {
        if (!isSeatBusy(seat)) {
            throw new GameSystemException("Passed seat does not exist at the table");
        }
        table.removeSeat(seat);
    }

    public boolean isSeatBusy(Seat seat) {
        return getTable().isSeatBusy(seat.getSeatNumber());
    }

    public void sendPhaseUpdateToPlayer(Seat seat) {
        if (getTable().isThereSeatWithBetForPlayer(seat.getPlayerUUID())) {//TODO think over it - it doesnt work properly
            messageSender.sendToClient(seat.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.READY_TO_GAME, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
        } else if (getTable().isThereSeatForPlayer(seat.getPlayerUUID())) {
            messageSender.sendToClient(seat.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
        } else
            messageSender.sendToClient(seat.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.EMPTY_TABLE, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
    }


    public void replaceSeatAndUpdateBetAtTheTable(Seat seatForBetUpdating) throws GameSystemException {
        List<Seat> seats = getTable().getSeats();

        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).equalsExcludingCurrentBet(seatForBetUpdating)) {
                seats.set(i, seatForBetUpdating);
                return;
            }
        }

        throw new GameSystemException("Seat wasn't changed. No matching seat found at the table");
    }

    public List<Seat> getCalculatedGameSeats() {
        return table.getCalculatedGameSeats();
    }

    public List<Player> getPlayersWhoAreInGame() {
        return null;
    }

    public void addPlayerNickName(Player player) {
        table.addPlayerNickName(player);
    }
}
