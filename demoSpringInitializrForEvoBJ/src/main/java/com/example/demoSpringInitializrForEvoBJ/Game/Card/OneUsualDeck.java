package com.example.demoSpringInitializrForEvoBJ.Game.Card;

import java.util.ArrayList;
import java.util.List;

public class OneUsualDeck {
    private static final int COUNT_OF_INITIAL = 13;

    private List<Card> oneUsualDeck = new ArrayList<>();

    public OneUsualDeck() {
        createOneUsualDeck();
    }

    public List<Card> getOneUsualDeck() {
        return oneUsualDeck;
    }

    private void createOneUsualDeck() {
        int count = 0;
        ECardSuits[] suits = ECardSuits.values();
        for (ECardSuits suit : suits) {
            String currentSuit = "";
            int coefficient = 0;
            switch(suit){
                case HEARTS: currentSuit = "Hearts";
                    break;

                case CLUBS: currentSuit = "Clubs";
                    break;

                case DIAMONDS: currentSuit = "Diamonds";
                    break;

                case SPADES: currentSuit = "Spades";
                    break;

            }
            for (int init = 2; init < COUNT_OF_INITIAL+2; ++init) {
                String currentInit = String.valueOf(init);
                if(init >= 2 && init <= 10){
                    coefficient = init;
                }


                if (init == 11) {
                    currentInit = "Jack";
                    coefficient = 10;
                }
                if (init == 12) {
                    currentInit = "Quin";
                    coefficient = 10;
                }
                if (init == 13) {
                    currentInit = "King";
                    coefficient = 10;
                }
                if (init == 14) {
                    currentInit = "Ace";
                    coefficient = 11;
                }
                oneUsualDeck.add(new Card(currentInit, currentSuit, coefficient));
                /*++count;
                System.out.println(count + ") Card - " + currentInit + " " + currentSuit);*/
            }
        }
    }

    public void printUsualDeck(){
        int count = 1;
        for(Card card: oneUsualDeck){
            System.out.println(count + ") " + card.getInitial() + " of " + card.getSuit());
            ++count;
        }
    }
}
