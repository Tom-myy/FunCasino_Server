package com.example.demoSpringInitializrForEvoBJ.game.model.card;

public class Card {
    private String initial;
    private String suit;
    private int coefficient;

    public int getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(int coefficient) {
        this.coefficient = coefficient;
    }


    public Card(String initial, String suit, int coefficient) {
        this.initial = initial;
        this.suit = suit;
        this.coefficient = coefficient;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }
}
