package com.example.demoSpringInitializrForEvoBJ.Game.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import com.example.demoSpringInitializrForEvoBJ.Game.Card.*;
import com.example.demoSpringInitializrForEvoBJ.Game.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Seat {



    //Business logic
    @Getter @Setter
    private String playerUUID;
    @Getter
    private int seatNumber;


    public void fullSeatReset(){
        mainScore = 0;
        currentBet = 0;
        mainHand = new ArrayList<>();
        additionalHandForSplit = new ArrayList<>();
        lastDecision = null;
        gameResultStatus = null;
        isAceUsed = false;
    }
    //Bet logic
    @Getter
    private int mainScore = 0;//TODO think about changing score at the moment getting new card in hand...

    @Getter
    private int additionalScore = 0;
    @Setter
    @Getter
    private int currentBet = 0;
    @Getter
    private List<Card> mainHand = new ArrayList<>();
    @Setter
    @Getter
    private List<Card> additionalHandForSplit = new ArrayList<>();
    @Setter
    @Getter
    private EDecision lastDecision = null;
    @Getter
    @Setter
    private EDecision currentDecision = null;
    @Getter
    @Setter
    private EGameResultStatus gameResultStatus = null;
    @Getter
    @Setter
    private boolean isBJ = false;
    @Getter
    @Setter
    private boolean isEnsured = false;
    private String aceScore = "0/0";
    public String getTwoAceScore() {
        return aceScore;
    }
    public void resetAceScore() {
        aceScore = "0/0";
    }


    public Seat() {
    }

    public Seat(String playerUUID, int seatNumber) {
        this.playerUUID = playerUUID;
        this.seatNumber = seatNumber;
    }

    @JsonIgnore
    public Seat getSeat(){
        return this;
    }






    @JsonIgnore
    private static final int MINIMUM_ACE_SUMMAND = 1;
    @JsonIgnore
    private boolean isAceUsed = false;

    @JsonIgnore
    public void calculateScore(Card card) {
        mainHand.add(card);

        int score = mainScore;

        if(card.getInitial().equalsIgnoreCase("Ace")){
            if(isAceUsed)
                score += MINIMUM_ACE_SUMMAND;
            else
                score += card.getCoefficient();
        } else
            score += card.getCoefficient();

        boolean hasAce = mainHand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if(score > 21 && hasAce && !isAceUsed) {
            score -= 10;
            isAceUsed = true;
        }

        mainScore = score;

        if(mainScore > 21)
            setGameResultStatus(EGameResultStatus.TOO_MANY);
    }

    @JsonIgnore
    public void calculateScoreForSplit(Card card) {
        mainHand.add(card);

        int score = mainScore;

        if(card.getInitial().equalsIgnoreCase("Ace")){
            if(isAceUsed)
                score += MINIMUM_ACE_SUMMAND;
            else
                score += card.getCoefficient();
        } else
            score += card.getCoefficient();

        boolean hasAce = mainHand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if(score > 21 && hasAce && !isAceUsed) {
            score -= 10;
            isAceUsed = true;
        }

        mainScore = score;

        if(mainScore > 21)
            setGameResultStatus(EGameResultStatus.TOO_MANY);
    }






//    public Seat(String playerIdenf, int seatNumber) {
//        this.playerIdenf = playerIdenf;
//        this.seatNumber = seatNumber;
//    }

    public void resetGameResultStatus() {
        gameResultStatus = EGameResultStatus.PROGRESSING;
    }

    public void changeMainScore(int score) {
        this.mainScore += score;
    }
    public void changeAdditionalScore(int score) {
        this.additionalScore += score;
    }

    public void resetScore() {
        this.mainScore = 0;
    }

    public Card getCurrentCardInHandByIndex(int i) {//TODO prevent array out of range
        return mainHand.get(i);
    }

    public void addOneCardInHand(Card card) {
        mainHand.add(card);
    }

    public void resetCardsInHand() {
        mainHand = new ArrayList<>();
    }

/*    public void doubleDownTheBet() {
//        player.setAmountOfMoney(player.getAmountOfMoney() - currentBet);//TODO im not sure if it's correct to use player here
//
//        currentBet *= 2;
    }*/

    public void placeABet(double bet) {
//        player.setAmountOfMoney(player.getAmountOfMoney() - bet);//TODO im not sure if it's correct to use player here
//        currentBet = bet;
    }

    public boolean equalsExcludingCurrentBet(Seat seat) {
        if (seat == null) return false;

        return seatNumber == seat.seatNumber &&
                mainScore == seat.mainScore &&
                Objects.equals(playerUUID, seat.playerUUID) &&
                Objects.equals(lastDecision, seat.lastDecision) &&
                gameResultStatus == seat.gameResultStatus &&
                Objects.equals(mainHand, seat.mainHand) &&
                Objects.equals(aceScore, seat.aceScore);
    }

    public boolean equalsBySeatNumberAndUUID(Seat seat) {
        if (seat == null) return false;

        return seatNumber == seat.seatNumber &&
                mainScore == seat.mainScore &&
                Objects.equals(playerUUID, seat.playerUUID);
    }

    public void printMoneyInfo(){
        System.out.println("seat=" + seatNumber + ", bet=" + currentBet);
    }
}
