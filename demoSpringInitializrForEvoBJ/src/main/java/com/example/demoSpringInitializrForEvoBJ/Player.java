package com.example.demoSpringInitializrForEvoBJ;

import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.Game.Table.Seat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Player {

    @Getter
    @Setter
    @Delegate
    private EvoUserDTO evoUserDTO;
    @Getter
    private List<Seat> seats = new ArrayList<>();

    @Getter
    @Setter
    private boolean inTheGame = false;
    @Getter
    @Setter
    private boolean sideBet = false;
    @Getter
    @Setter
    @JsonIgnore
    private boolean wantsToStartGame = false;

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public Player() {}

    public UUID getPlayerUUID(){
        return evoUserDTO.getPlayerUUID();
    }


    public void resetBalanceDifference(){
        evoUserDTO.setBalanceDifference(BigDecimal.ZERO);
    }

    @JsonIgnore
    public BigDecimal getBalanceDifference() {
        return evoUserDTO.getBalanceDifference();
    }

/*    public void changeBalanceDifference(int amount) {
        evoUserDTO.changeBalanceDifference(amount);
    }*/

    @JsonIgnore
    public BigDecimal getTotalBet() {
        if(seats.isEmpty() || seats == null) return BigDecimal.ZERO;

//        int totalBet = 0;
        BigDecimal totalBet = BigDecimal.ZERO;
        for (Seat seat : seats) {
//            totalBet += seat.getCurrentBet();
            totalBet = totalBet.add(seat.getCurrentBet());
        }
        return totalBet;
    }

//    private boolean isValidDecision(String decision) {
//        return decision.equalsIgnoreCase("hit") ||
//                decision.equalsIgnoreCase("double-down") ||
//                decision.equalsIgnoreCase("cash-out") ||
//                decision.equalsIgnoreCase("stand");
//    }
}
