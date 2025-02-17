package com.example.demoSpringInitializrForEvoBJ.Game.Table;

import com.example.demoSpringInitializrForEvoBJ.Game.Dealer;
import com.example.demoSpringInitializrForEvoBJ.Game.EGamePhaseForInterface;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.example.demoSpringInitializrForEvoBJ.PlayersBroadcastCallback;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Table {

    @JsonIgnore
    private static final Logger logger = LoggerFactory.getLogger(Table.class);
    @JsonIgnore
    private static PlayersBroadcastCallback playersBroadcastCallback;//It's for sending to certain player his player data
    @JsonIgnore
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
    private Map<String, String> playerNickNames = new HashMap<>();//<playerUUID, playerNickName>

    @JsonIgnore
    public void addPlayerNickName(Player player) {
        if(playerNickNames.containsKey(player.getPlayerUUID())) {
            logger.error("PlayerUUID already exists in playerNickNames!");
            return;
        }

        playerNickNames.put(player.getPlayerUUID(), player.getNickName());
    }

    @JsonIgnore
    public void removePlayerNickName(Player player) {
        if(!playerNickNames.containsKey(player.getPlayerUUID())) {
            logger.error("PlayerUUID doesn't exist in playerNickNames!");
            return;
        }

        playerNickNames.remove(player.getPlayerUUID());
    }

    @JsonIgnore
    public Table(List<Player> players, PlayersBroadcastCallback callback) {
        this.players = players;
        this.playersBroadcastCallback = callback;
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
    public void removePlayerAtTheTableByKey(int key) {
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

    @JsonIgnore
    public boolean isThereGameSeat() {
        List<Seat> gameSeats = new ArrayList<>();
        for (Seat seat : seats) {
            if (seat.getCurrentBet() > 0)
                gameSeats.add(seat);
        }

        return !gameSeats.isEmpty();
    }

    @JsonIgnore
    public List<Seat> getAndSetGameSeats() {
        gameSeats = new CopyOnWriteArrayList<>();
        for (Seat seat : seats) {
            if (seat.getCurrentBet() > 0) {
                gameSeats.add(seat);//TODO mb change it to GameSeatsCollection

/*                for (Player player : players) {//TODO change players' collection to Map
                    if (player.getPlayerUUID().equals(seat.getPlayerUUID())) {
                        player.changeBalance(-seat.getCurrentBet());
                    }
                }*/
            }
        }

        gameSeats.sort(Comparator.comparing(Seat::getSeatNumber));

/*        if (playersBroadcastCallback != null) {//TODO can't get why i need it, as for me it's pointless
            playersBroadcastCallback.playersBroadcast();
        } else logger.error("playersBroadcastCallback is null");*/

        return gameSeats;
    }

    @JsonIgnore
    public boolean isThereSeatWithBetForPlayer(String playerUUID) {
        for (Seat seat : seats) {
            if (seat.getPlayerUUID().equals(playerUUID) && seat.getCurrentBet() > 0) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean isThereSeatForPlayer(String playerUUID) {
        for (Seat seat : seats) {
            if (seat.getPlayerUUID().equals(playerUUID)) {
                return true;
            }
        }
        return false;
    }


/*    public Seat getSeatByKey(int key) {
        if (key >= 0 && key <= 6)
            return seats.get(key);
        else return null;
    }*/
}

