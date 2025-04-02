package com.example.demoSpringInitializrForEvoBJ;

import com.example.demoSpringInitializrForEvoBJ.Game.Table.Seat;
import com.example.demoSpringInitializrForEvoBJ.Message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.myPackage.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PlayerService {
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);
    private final PlayerRegistry playerRegistry;
    private final MessageSender messageSender;

    public PlayerService(PlayerRegistry playerRegistry, MessageSender messageSender) {
        this.playerRegistry = playerRegistry;
        this.messageSender = messageSender;
    }

    public void addSeat(Seat seat) throws GameSystemException {
        Player player = playerRegistry.findPlayerByUUID(seat.getPlayerUUID());
        if (player == null) {
            throw new GameSystemException("Player not found in playerRegistry during seat adding (playerUUID = " + seat.getPlayerUUID() + ")");
        }
        player.addSeat(seat);
        messageSender.sendToClient(seat.getPlayerUUID(), new MyPackage<>(player, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
    }

    public Player removeSeatAndRefund(Seat seat) throws GameSystemException {
        Player player = getPlayerByUUIDOrThrow(seat.getPlayerUUID());
        Seat seatOfPlayer = findSeatOfPlayerOrThrow(player, seat);

        player.changeBalance(seatOfPlayer.getCurrentBet());
        player.getSeats().remove(seatOfPlayer);

        return player;
    }

    private Seat findSeatOfPlayerOrThrow(Player player, Seat seatRef) throws GameSystemException {
        return player.getSeats().stream()
                .filter(s -> s.equalsBySeatNumberAndUUID(seatRef))
                .findFirst()
                .orElseThrow(() -> new GameSystemException("Seat not found in player's collection"));
    }

    public Player getPlayerByUUIDOrThrow(UUID playerUUID) throws GameSystemException {
        Player player = playerRegistry.findPlayerByUUID(playerUUID);
        if (player == null) {
            throw new GameSystemException("Player not found in playerRegistry (playerUUID = " + playerUUID + ")");
        }
        return player;
    }

    public Player replaceSeatAndUpdateBetInPlayer(Seat seat) throws GameSystemException {//TODO not sure that it work correctly...
        Player player = getPlayerByUUIDOrThrow(seat.getPlayerUUID());
        Seat oldSeat = findSeatOfPlayerOrThrow(player, seat);

        int seatIndex = player.getSeats().indexOf(oldSeat);
        if (seatIndex == -1) {
            throw new GameSystemException("Seat to replace not found in player's seat list (playerUUID=" + player.getPlayerUUID() + ")");
        }

        BigDecimal oldBet = oldSeat.getCurrentBet();
        BigDecimal newBet = seat.getCurrentBet();

        player.getSeats().set(seatIndex, seat);
        player.changeBalance(newBet.subtract(oldBet).negate());

        return player;
    }
}
