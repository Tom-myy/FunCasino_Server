package com.example.demoSpringInitializrForEvoBJ.Game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.util.*;
import com.example.demoSpringInitializrForEvoBJ.Game.Card.*;

public class Dealer {

    @Getter
    private String nickName = "Dealer";
    @Getter
    @Setter
    private double money = 0;
    @Getter
    private int score = 0;
    @Getter
    @Setter
    private EGameResultStatus gameResultStatus;
    @Getter
    @Setter
    private boolean isBJ = false;
    @Getter
    @Setter
    private boolean isThereHiddenCard = false;
    @Getter
    private List<Card> hand = new ArrayList<>();
    @JsonIgnore
    private Card hiddenCard = null;

    @JsonIgnore
    public Card getHiddenCard() {
        isThereHiddenCard = false;
        Card hidden = hiddenCard;
        hiddenCard = null;
        return hidden;
    }
    @JsonIgnore
    public void setHiddenCard(Card hiddenCard) {
        this.hiddenCard = hiddenCard;
        isThereHiddenCard = true;
    }



    public Card getCurrentCardInHandByIndex(int i) {//TODO prevent array out of range
        return hand.get(i);
    }

    public Dealer() {}

/*    public void resetGameResultStatus(){
        gameResultStatus = null;//changed from PROGRESSING to null
    }*/

/*    public void changeScore(int score) {
        this.score += score;
    }*/

/*    public void resetScore() {
        this.score = 0;
    }*/

/*    public void addOneCardInHand(Card card) {
        hand.add(card);
    }*/

/*    public void resetCardsInHand() {
        hand = new ArrayList<>();
    }*/

    @JsonIgnore
    public void fullSeatReset(){
        score = 0;
        hand = new ArrayList<>();
        gameResultStatus = null;
        isAceUsed = false;
    }

    @JsonIgnore
    private static final int MINIMUM_ACE_SUMMAND = 1;

    @JsonIgnore
    private boolean isAceUsed = false;

    @JsonIgnore
    public void calculateScore(Card card) {
        hand.add(card);

        int tmpScore = score;

        if(card.getInitial().equalsIgnoreCase("Ace")){
            if(isAceUsed)
                tmpScore += MINIMUM_ACE_SUMMAND;
            else
                tmpScore += card.getCoefficient();
        } else
            tmpScore += card.getCoefficient();

        boolean hasAce = hand.stream().anyMatch(cardFromHand -> cardFromHand.getInitial().equalsIgnoreCase("Ace"));

        if(tmpScore > 21 && hasAce && !isAceUsed) {
            tmpScore -= 10;
            isAceUsed = true;
        }

        score = tmpScore;

        if(score > 21)
            setGameResultStatus(EGameResultStatus.TOO_MANY);
    }
}