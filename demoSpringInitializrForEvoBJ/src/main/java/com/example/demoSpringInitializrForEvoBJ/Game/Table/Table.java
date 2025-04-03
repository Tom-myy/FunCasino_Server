package com.example.demoSpringInitializrForEvoBJ.Game.Table;

import com.example.demoSpringInitializrForEvoBJ.Game.Dealer;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Table {

    @JsonIgnore
    private static final Logger logger = LoggerFactory.getLogger(Table.class);

    @JsonIgnore
    @Getter
    private List<Player> players = new LinkedList<>();//for money management

    //Common fields:
    @Getter
    private List<Seat> seats = new ArrayList<>();
    @Getter
    private List<Seat> gameSeats = new ArrayList<>();
    @Setter
    @Getter
    private Dealer dealer = null;
    @Getter
    @Setter
    private boolean isGame = false;
/*    @Getter
    @Setter
    private EGamePhaseForInterface gamePhase = null;*/

    @Getter
    private Map<UUID, String> playerNickNames = new HashMap<>();//<playerUUID, playerNickName>

    @JsonIgnore
    public void addPlayerNickName(Player player) {
        if (playerNickNames.containsKey(player.getPlayerUUID())) {
            logger.error("PlayerUUID already exists in playerNickNames!");
            return;
        }

        playerNickNames.put(player.getPlayerUUID(), player.getNickName());
    }

    @JsonIgnore
    public void removePlayerNickName(Player player) {
        if (!playerNickNames.containsKey(player.getPlayerUUID())) {
            logger.error("PlayerUUID doesn't exist in playerNickNames!");
            return;
        }

        playerNickNames.remove(player.getPlayerUUID());
    }

    @JsonIgnore
    public Table(List<Player> players/*, PlayersBroadcastCallback callback*//*, MessageSender messageSender*/) {
        this.players = players;
//        this.playersBroadcastCallback = callback;
//        this.messageSender = messageSender;
    }

    //Common
    public boolean isSeatBusy(int seatNumber) {
        for (Seat seat : seats) {
            if (seat.getSeatNumber() == seatNumber)
                return true;
        }
        return false;
    }


    @JsonIgnore
    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    @JsonIgnore
    public void removeSeat(Seat seat) {
        Integer index = null;
        for (Seat s : seats) {
            if(s.getSeatNumber() == seat.getSeatNumber()) {
                index = seats.indexOf(s);
                break;
            }
        }

        if(index != null) {
            seats.remove(index.intValue());
        } else {
            logger.error("Seat doesn't exist in seats!");
        }
    }

    @JsonIgnore
    public void removeSeatAtTheTableByKey(int key) {
        int index = -1;

        for (Seat seat : seats) {
            if (seat.getSeatNumber() == key) {
                index = seats.indexOf(seat);
            }
        }

        if (index != -1) {
            seats.remove(index);
        } else System.err.println("There is no such seat with key " + key);
    }

/*    @JsonIgnore
    public boolean isThereGameSeat() {
        List<Seat> gameSeats = new ArrayList<>();
        for (Seat seat : seats) {
//            if (seat.getCurrentBet() > 0)
            if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0)
                gameSeats.add(seat);
        }

        return !gameSeats.isEmpty();
    }*/

    @JsonIgnore
    public List<Seat> getCalculatedGameSeats() {
        List<Seat> calculatedGameSeats = new CopyOnWriteArrayList<>();
        for (Seat seat : seats) {
            if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                calculatedGameSeats.add(seat);
            }
        }
        calculatedGameSeats.sort(Comparator.comparing(Seat::getSeatNumber));

        return calculatedGameSeats;
    }

    @JsonIgnore
    public List<Seat> getAndSetGameSeats() {
        gameSeats = getCalculatedGameSeats();

        return gameSeats;
    }
    /*    @JsonIgnore
        public List<Seat> getAndSetGameSeats() {
            gameSeats = new CopyOnWriteArrayList<>();
            for (Seat seat : seats) {
                if (seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                    gameSeats.add(seat);//TODO mb change it to GameSeatsCollection
                }
            }
            gameSeats.sort(Comparator.comparing(Seat::getSeatNumber));

            return gameSeats;
        }*/


    @JsonIgnore
    public boolean isThereSeatWithBetForPlayer(UUID playerUUID) {
        for (Seat seat : seats) {
//            if (seat.getPlayerUUID().equals(playerUUID) && seat.getCurrentBet() > 0) {
            if (seat.getPlayerUUID().equals(playerUUID) && seat.getCurrentBet().compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean isThereSeatForPlayer(UUID playerUUID) {
        for (Seat seat : seats) {
            if (seat.getPlayerUUID().equals(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public void removePlayersSeatsAtTheTable(Player player) {
        for (int i = 0; i < player.getSeats().size(); i++) {
            int index = -1;

            for (Seat seat : seats) {
                if (seat.getPlayerUUID().equals(player.getPlayerUUID())) {
                    index = seats.indexOf(seat);
                    break;
                }
            }

            if (index != -1) {
                seats.remove(index);
            } else System.err.println("There is no such seats");
        }
    }
/*    public Seat getSeatByKey(int key) {
        if (key >= 0 && key <= 6)
            return seats.get(key);
        else return null;
    }*/
}

