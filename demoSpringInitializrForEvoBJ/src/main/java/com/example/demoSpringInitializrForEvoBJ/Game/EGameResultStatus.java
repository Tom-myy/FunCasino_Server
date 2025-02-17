package com.example.demoSpringInitializrForEvoBJ.Game;

public enum EGameResultStatus {
    NOT_IN_GAME,
    PROGRESSING,//TODO think over it better (mb i need to use it only then, when seat are in a game, but before any gameResults)


    BLACKJACK,
    CASHED_OUT,
    TOO_MANY,

    WON,
    PUSHED,
    LOST
}
