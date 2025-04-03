package com.example.demoSpringInitializrForEvoBJ.game.model;

import com.example.demoSpringInitializrForEvoBJ.game.EDecision;
import com.example.demoSpringInitializrForEvoBJ.game.EGamePhaseForInterface;
import com.example.demoSpringInitializrForEvoBJ.game.EGameResultStatus;
import com.example.demoSpringInitializrForEvoBJ.game.model.card.Card;
import com.example.demoSpringInitializrForEvoBJ.game.model.card.OneUsualDeck;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.timer.TimerType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.timer.DecisionTimeObserver;
import com.example.demoSpringInitializrForEvoBJ.timer.TimerService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private static final int TIME_FOR_DECISION = 10;
    private static final int TIME_FOR_RESULT_ANNOUNCEMENT = 5000;
    private static final int TIME_BETWEEN_CARDS = 1000;
    private OneUsualDeck deckObject = new OneUsualDeck();
    private List<Card> gameDeck = null;
    private Dealer dealer;
    private Table table;
//    private MyTimer timer;
    private TimerService timerService;
    private PlayerRegistry playerRegistry;
    @Getter
    @Setter
    private boolean isGameRunning = false;
    private DecisionTimeObserver decisionTimeObserver;


/*    PlayersBroadcastCallback playersBroadcastCallback;//It's for sending to certain player his player data
    private GameToMessageHandlerListener listener;*/

    private final MessageSender messageSender;//TODO bad violation!

    private List<Player> players;//for money management


    private EGamePhaseForInterface gameStatusForInterface = EGamePhaseForInterface.EMPTY_TABLE;

    private static final int COUNT_OF_INITIAL_CARDS = 2;
    private static final int MINIMUM_ACE_SUMMAND = 1;
    private static final int MAXIMUM_ACE_SUMMAND = 11;


    private List<Seat> gameSeats;

    private EDecision decisionField = null;

    public EDecision getDecisionField() {
        EDecision decision = decisionField;

        decisionField = null;

        return decision;
    }

    public void setDecisionField(EDecision decision) {
        if (decision.equals(EDecision.HIT) ||
                decision.equals(EDecision.DOUBLE_DOWN) ||
                decision.equals(EDecision.SPLIT) ||
                decision.equals(EDecision.CASH_OUT) ||
                decision.equals(EDecision.STAND)) {

            decisionField = decision;
//            timer.stopTimer();
            timerService.stop(TimerType.DECISION_TIME);
        } else {
            System.err.println("Server got invalid decision: " + decision);
        }
    }

    public void changeGameStatusForInterface(EGamePhaseForInterface status) {
        gameStatusForInterface = status;
//        listener.broadcast(new MyPackage<>(gameStatusForInterface, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
        messageSender.broadcast(new MyPackage<>(gameStatusForInterface, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
    }

    public int getCountOfPlayersReadyForGame() {
        return gameSeats.size();
    }

    public Game(Table table, MessageSender messageSender, TimerService timerService, PlayerRegistry playerRegistry, DecisionTimeObserver decisionTimeObserver) {
//        this.listener = listener;
        this.table = table;
//        this.players = players;
//        this.playersBroadcastCallback = callback;
//        this.timer = timer;
//        this.messageSender = messageSender;
        this.messageSender = messageSender;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.decisionTimeObserver = decisionTimeObserver;
        players = playerRegistry.getPlayers();
    }

    public List<Player> startGame() {
        if (table.isGame()) {
            return null;
        } else {
            table.setGame(true);
            isGameRunning = true;
        }

//        timer.stopTimer();
        timerService.stop(TimerType.DECISION_TIME);


//        listener.broadcast(new MyPackage<>("", EMessageType.GAME_STARTED));
        gameSeats = table.getAndSetGameSeats();
        for (Seat s : gameSeats) {//TODO delete
            s.printMoneyInfo();
        }

        for (Seat s : gameSeats) {
            for (Player p : players) {
                if (p.getPlayerUUID().equals(s.getPlayerUUID())) {
                    p.setInTheGame(true);
                    messageSender.sendToClient(p.getPlayerUUID(), new MyPackage<>(p, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
                    break;
                }
//                p.resetBalanceDifference();
            }
        }

        messageSender.broadcast(new MyPackage<>(gameSeats, EMessageType.GAME_STARTED/*TABLE_STATUS*/));//mb send after resetGameResultStatus
//        listener.broadcast(new MyPackage<>(TIME_FOR_DECISION, EMessageType.TIME_FOR_DECISION));
        table.setDealer(new Dealer());
        dealer = table.getDealer();
        messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));//TODO mb not to send the dealer (//mb send after resetGameResultStatus)
//        String nextGame;

        if (gameDeck == null) {
            gameDeck = new ArrayList<>(deckObject.getOneUsualDeck());//TODO mb change smth here
            Collections.shuffle(gameDeck);
        }

        for (Seat seat : gameSeats) { //TODO mb change PROGRESSING to null
            seat.resetGameResultStatus();
        }
        messageSender.broadcast(new MyPackage<>(EGameResultStatus.PROGRESSING, EMessageType.E_GAME_RESULT_STATUS));

//        dealer.resetGameResultStatus(); //TODO mb change PROGRESSING to null
//        listener.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));//TODO mb not to send the dealer

        System.out.println("Bets are closed, good luck!");

        changeGameStatusForInterface(EGamePhaseForInterface.DEALING_CARDS);//TODO mb i dont need it

        System.out.println("\nInitial cards:");
        for (int i = 1; i <= COUNT_OF_INITIAL_CARDS; ++i) {
            for (Seat seat : gameSeats) {

                Card card = gameDeck.removeLast();

                seat.calculateScore(card);

                messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

                if (seat.getMainScore() == 21) {
                    System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                            "dealt to player on seat #" + seat.getSeatNumber() + ", score - BLACKJACK (" + seat.getMainScore() + ")");
                    //TODO display it in the players' interface

                    seat.setGameResultStatus(EGameResultStatus.BLACKJACK);
                    messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

                } else {
                    System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                            "dealt to player on seat #" + seat.getSeatNumber() + ", score = " + seat.getMainScore());
                }

                try {
                    Thread.sleep(TIME_BETWEEN_CARDS);//here was 1s
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            Card card = gameDeck.removeLast();


            if (i == 1) {
                System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                        "dealt to '" + dealer.getNickName() + "', score = " + dealer.getScore());

                dealer.calculateScore(card);
                messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));

            } else {//TODO do it more beautiful and smarter
                System.out.println("hidden card was dealt to '" + dealer.getNickName() + "'" +
                        ", score = " + dealer.getCurrentCardInHandByIndex(0).getCoefficient() + "+");

                dealer.setHiddenCard(card);
                messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));
            }

            try {
                Thread.sleep(TIME_BETWEEN_CARDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        changeGameStatusForInterface(EGamePhaseForInterface.CARDS_WERE_DEALT);

        changeGameStatusForInterface(EGamePhaseForInterface.DECISION_TIME);

//        EDecision firstDecision;
        System.out.println("\nPlayer's decisions:");


        for (Seat seat : gameSeats) {

            while (seat.getMainScore() < 21) {

                EDecision firstDecision = gettingDecision(seat);

                try {//имитация того, что дилер берёт карту
                    Thread.sleep(TIME_BETWEEN_CARDS);//1
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (firstDecision == null) {
                    logger.error("Smth went wrong and decision is null");
                    new RuntimeException("Smth went wrong and decision is null");
                    return null;
                }

                if (firstDecision.equals(EDecision.STAND)) {
                    seat.setLastDecision(firstDecision);
                    messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));
                    System.out.println(seat.getPlayerUUID() + " decided to " + firstDecision);

                    System.out.println(seat.getPlayerUUID() + " is standing on " + seat.getMainScore());

                    break;

                } else if (firstDecision.equals(EDecision.HIT)) {
                    seat.setLastDecision(firstDecision);

                    System.out.println(seat.getPlayerUUID() + " decided to " + firstDecision);

                    EDecision nextDecision;
                    boolean isStand = false;

                    do {
                        Card card = gameDeck.removeLast();


                        seat.calculateScore(card);


                        System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                                "dealt to '" + seat.getPlayerUUID() + "'");

                        messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

                        if (seat.getMainScore() < 21) {
                            do {
                                System.out.print(seat.getPlayerUUID() + " has " + seat.getMainScore() + ", what is your next decision? (hit, cash-out, stand) - ");//todo сделать, чтобы это предлагал дилер

                                nextDecision = gettingDecision(seat);
                                //TODO по идее нужна проверка на налл как и с первым решением
                                seat.setLastDecision(nextDecision);

                                if (nextDecision.equals(EDecision.STAND)) {
                                    System.out.println(seat.getPlayerUUID() + " is standing on " + seat.getMainScore());
                                    isStand = true;

                                    break;
                                } else if (nextDecision.equals(EDecision.CASH_OUT)) {
                                    System.out.println(seat.getPlayerUUID() + " CASHOUT");
                                    isStand = true;
                                    seat.setGameResultStatus(EGameResultStatus.CASHED_OUT);
                                    messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

                                    break;
                                }
                            } while (!isValidNextDecision(nextDecision));
                        } else {
                            isStand = true;
                        }

                    } while (!isStand);

                    break;

                } else if (firstDecision.equals(EDecision.DOUBLE_DOWN)) {

                    //money
                    Player curPlayer = null;
                    for (Player player : players) {
                        if (player.getPlayerUUID().equals(seat.getPlayerUUID())) {
                            curPlayer = player;
                        }
                    }

                    if (curPlayer == null) {
                        logger.error("curPlayer is null");
                        return null;
                    }

//                    curPlayer.changeBalance(-seat.getCurrentBet());//balance was changed
                    curPlayer.changeBalance(seat.getCurrentBet().negate());//balance was changed

                    Seat tmpSeat = null;
                    int tmpInd = -1;
                    for (Seat s : curPlayer.getSeats()) {
                        if (seat.getSeatNumber() == s.getSeatNumber()) {
                            tmpSeat = s;
                            tmpInd = curPlayer.getSeats().indexOf(s);
                        }
                    }

                    if (tmpSeat == null || tmpInd == -1) {
                        logger.error("tmpSeat or tmpInd is wrong");
                        return null;
                    }

                    curPlayer.getSeats().set(tmpInd, seat);

//                    seat.setCurrentBet(seat.getCurrentBet() * 2);
                    seat.setCurrentBet(seat.getCurrentBet().multiply(BigDecimal.valueOf(2)));
                    playersBroadcast();//TODO think here, coz in fact i dont need broadcast (i change only one Player)

                    seat.setLastDecision(firstDecision);
                    System.out.println(seat.getPlayerUUID() + " decided to " + firstDecision);

                    Card card = gameDeck.removeLast();

                    seat.calculateScore(card);

                    messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

                    System.out.println(firstDecision + " for " + seat.getPlayerUUID());
                    System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                            "dealt to '" + seat.getPlayerUUID() + "'");


                    if (seat.getMainScore() < 21) {
                        System.out.println(seat.getPlayerUUID() + " has " + seat.getMainScore());
                    }

                    break;

                } else if (firstDecision.equals(EDecision.SPLIT)) {

                    //TODO finish this split option
                    //TODO finish this split option

                    seat.setLastDecision(firstDecision);
                    System.out.println(seat.getPlayerUUID() + " decided to " + firstDecision);
                    System.out.println(firstDecision + " for " + seat.getPlayerUUID());

                    //replacing 1st cards
                    seat.getAdditionalHandForSplit().add(seat.getMainHand().getLast());//take card from main hand and put in additional hand
                    seat.getMainHand().removeLast();//remove 'put card in additional hand' from main hand

                    //changing mainScore (according only 1 card in hand)
                    seat.changeMainScore(-seat.getAdditionalHandForSplit().getLast().getCoefficient());//change mainScore (minus last card)

                    //changing additionalScore (according only 1 card in hand)
                    seat.changeAdditionalScore(seat.getAdditionalHandForSplit().getLast().getCoefficient());

                    //adding one more card in each hand (initial cards for split)
                    seat.getMainHand().add(gameDeck.removeLast());
                    seat.getAdditionalHandForSplit().add(gameDeck.removeLast());

                    //changing both mainScore and additionalScore (according full initial cards)
                    seat.changeMainScore(seat.getMainHand().getLast().getCoefficient());
                    seat.changeAdditionalScore(seat.getAdditionalHandForSplit().getLast().getCoefficient());

                    //i need to take the player with this seat and change his old seat to new one
                    Player splitPlayer = null;
                    for (Player p : players) {
                        if (p.getPlayerUUID().equals(seat.getPlayerUUID())) {
                            splitPlayer = p;
                        }
                        break;
                    }

                    if (splitPlayer == null) {
                        logger.error("splitPlayer is null");
                        return null;
                    }

                    int splitInd = -1;
                    for (Seat s : splitPlayer.getSeats()) {
                        if (seat.getSeatNumber() == s.getSeatNumber()) {
                            splitInd = splitPlayer.getSeats().indexOf(s);
                        }
                        break;
                    }

                    if (splitInd == -1) {
                        logger.error("splitInd is null");
                        return null;
                    }

                    splitPlayer.getSeats().set(splitInd, seat);

//                    splitPlayer.changeBalance(-seat.getCurrentBet());//balance was changed
                    splitPlayer.changeBalance(seat.getCurrentBet().negate());//balance was changed

                    //need to think over the bet for the split and need to send this player

                    //
                    //
                    //

                } else if (firstDecision.equals(EDecision.CASH_OUT)) {
                    seat.setLastDecision(firstDecision);
                    System.out.println(seat.getPlayerUUID() + " cashed-out");
                    seat.setGameResultStatus(EGameResultStatus.CASHED_OUT);
                    messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

                    break;
                }

            }
            if (seat.getMainScore() == 21 && seat.getMainHand().size() == 2) {
                System.out.println(seat.getPlayerUUID() + " has BLACKJACK (" + seat.getMainScore() + ") - amazing");

                seat.setGameResultStatus(EGameResultStatus.BLACKJACK);//тк если у диллера тоже BJ, то у игрока PUSH
                messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

            }
            if (seat.getMainScore() == 21 && seat.getMainHand().size() > 2) {
                System.out.println(seat.getPlayerUUID() + " has " + seat.getMainScore() + " - good catch");
                messageSender.broadcast(new MyPackage<>(seat, EMessageType.CHANGED_SEAT_FOR_GAME));

            }
            if (seat.getMainScore() > 21) {
                System.out.println(seat.getPlayerUUID() + " has TOO MANY (" + seat.getMainScore() + ") - sadly");

                seat.setGameResultStatus(EGameResultStatus.TOO_MANY);//как по мне - не особо правильно это тут распологать

            }

        }
        changeGameStatusForInterface(EGamePhaseForInterface.DEALER_DECISION);

        try {
            Thread.sleep(TIME_BETWEEN_CARDS);//1.5s but it's not TIME_BETWEEN_CARDS
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        dealer.calculateScore(dealer.getHiddenCard());
        messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));//TODO is this necessary?
        try {
            Thread.sleep(TIME_BETWEEN_CARDS);//1s but it's not TIME_BETWEEN_CARDS
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //check dealer's score and then appropriate actions (hit or stand)
        while (dealer.getScore() < 17) {

            System.out.println("hit for 'Dealer'");
            Card card = gameDeck.removeLast();
            dealer.calculateScore(card);
            messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));
            System.out.println(card.getInitial() + " of " + card.getSuit() + " was " +
                    "dealt to '" + dealer.getNickName() + "', score - " + dealer.getScore());

            if (dealer.getScore() < 17) {
                try {
                    Thread.sleep(TIME_BETWEEN_CARDS);//1s but it's not TIME_BETWEEN_CARDS
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        {//this block of code is for dealer's turn (after players)...
            System.out.println("Game results:");
            changeGameStatusForInterface(EGamePhaseForInterface.RESULT_ANNOUNCEMENT);

            //check dealer's score - BJ
            if (dealer.getScore() == 21 && dealer.getHand().size() == 2) {
                System.out.println("Unfortunately, " + dealer.getNickName() + " has BLACKJACK (" + dealer.getScore() + ")");

                dealer.setGameResultStatus(EGameResultStatus.BLACKJACK);
                messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));


                gameSeats.stream()//TODO develop insurance if Dealer has ace
                        .filter(p -> p.getGameResultStatus() == EGameResultStatus.BLACKJACK)
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.PUSHED));

                gameSeats.stream()
                        .filter(p -> p.getGameResultStatus() == EGameResultStatus.PROGRESSING)
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.LOST));
            }

            //check dealer's score - less or equals 20
            if (dealer.getScore() <= 20) {
                System.out.println(dealer.getNickName() + " has " + dealer.getScore());

                gameSeats.stream()
                        .filter(p -> p.getMainScore() < dealer.getScore() &&
                                p.getGameResultStatus() == EGameResultStatus.PROGRESSING)//отметка, что он к примеру не кєшанул
                        //или тп, а ещё в игре
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.LOST));

                gameSeats.stream()
                        .filter(p -> p.getMainScore() == dealer.getScore() &&
                                p.getGameResultStatus() == EGameResultStatus.PROGRESSING)//отметка, что он к примеру не кєшанул
                        //или тп, а ещё в игре
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.PUSHED));

                gameSeats.stream()
                        .filter(p -> p.getMainScore() > dealer.getScore() &&
                                p.getGameResultStatus() == EGameResultStatus.PROGRESSING)
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.WON));
            }

            //check dealer's score - quells 21 and not BJ
            if (dealer.getScore() == 21 && dealer.getHand().size() > 2) {
                System.out.println(dealer.getNickName() + " has " + dealer.getScore());

                gameSeats.stream()
                        .filter(p -> p.getMainScore() == 21 &&
                                p.getGameResultStatus() == EGameResultStatus.PROGRESSING)
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.PUSHED));

                gameSeats.stream()
                        .filter(p -> p.getGameResultStatus() == EGameResultStatus.PROGRESSING)
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.LOST));
            }

            //check dealer's score - more than 21 (too many)
            if (dealer.getScore() > 21) {
                System.out.println(dealer.getNickName() + " has TOO MANY (" + dealer.getScore() + ")");

                dealer.setGameResultStatus(EGameResultStatus.TOO_MANY);
                messageSender.broadcast(new MyPackage<>(dealer, EMessageType.DEALER));


                gameSeats.stream()
                        .filter(p -> p.getMainScore() <= 21 &&
                                p.getGameResultStatus() == EGameResultStatus.PROGRESSING)
                        .forEach(p -> p.setGameResultStatus(EGameResultStatus.WON));
            }
        }

        //output the dealer's game results to the console
        if (dealer.getGameResultStatus() == EGameResultStatus.BLACKJACK ||
                dealer.getGameResultStatus() == EGameResultStatus.TOO_MANY) {//TODO что-то я не понял почему тут BJ и TM в однои if-е...

            System.out.println(dealer.getNickName() + " has " + dealer.getGameResultStatus());

        } else {
            System.out.println(dealer.getNickName() + " has " + dealer.getScore());
        }

        //output the seats' game results to the console
        gameSeats.forEach(s -> System.out.println("Player on seat" + s.getSeatNumber() + " - " + s.getGameResultStatus()));

        distributeMoney();
        //playersBroadcastCallback.playersBroadcast();//TODO as for me it's pointless coz i do it in distributeMoney() before this line

        messageSender.broadcast(new MyPackage<>(gameSeats, EMessageType.GAME_RESULTS));//broadcasting of gameSeats with last game data

        //this block is for checking amount of cards
        if (gameDeck.size() < ((gameSeats.size() + 1) * 4)) {
            System.err.println("There are few cards left in the shoe...");
            gameDeck = deckObject.getOneUsualDeck();
            Shuffler.myShuffle(gameDeck);
        }

        //delay after RESULT_ANNOUNCEMENT to give players time to see game results
        try {
            Thread.sleep(TIME_FOR_RESULT_ANNOUNCEMENT);//5k
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (Player p : players) {
            for (Seat s : p.getSeats()) {
                s.fullSeatReset();
            }
            p.setWantsToStartGame(false);
//            p.resetBalanceDifference();
        }

        playersBroadcast();//need because of s.fullSeatReset() for every player

        gameSeats = null;

        //reset dealer's game data
        dealer.fullSeatReset();


        for (Player player : players) {
            if (player.getSeats().isEmpty()) {
                messageSender.sendToClient(player.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.EMPTY_TABLE, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
            } else {
                messageSender.sendToClient(player.getPlayerUUID(), new MyPackage<>(EGamePhaseForInterface.PLACING_BETS, EMessageType.E_GAME_STATUS_FOR_INTERFACE));
            }
        }

        for (Player p : players) {
            p.setInTheGame(false);
            messageSender.sendToClient(p.getPlayerUUID(), new MyPackage<>(p, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
        }
        table.setDealer(null);//мб необязательно
        messageSender.broadcast(new MyPackage<>(table.getSeats(), EMessageType.GAME_FINISHED));//sending exactly busy seats at the table (not gameSeats)...
        //TODO надо сделать так, чтобы играющий не мог занять ещё места пока не закончится игра

        //TODO (по поводу верхнего я хз как оно) пока что в этой реализации сделано так, что когда GAME _ FINISHED, то отправляется коллекция обычных мест
        //TODO чтобы играющие (и наблюдающие за игрой) получили актуальный список мест

        table.setGame(false);
        isGameRunning = false;

        return players;
    }

    private boolean isValidNextDecision(EDecision decision) {
        return decision.equals(EDecision.HIT) ||
                decision.equals(EDecision.CASH_OUT) ||
                decision.equals(EDecision.STAND);
    }

    public void distributeMoney() {//TODO check if everything works properly
        if (players == null) {
            logger.error("Player collection is null");
            new Exception("Player collection is null").printStackTrace();
            return;
        }

        EGameResultStatus result;
        for (Seat seat : gameSeats) {
            result = seat.getGameResultStatus();

            Player curPlayer = null;
            for (Player player : players) {
                if (player.getPlayerUUID().equals(seat.getPlayerUUID())) {
                    curPlayer = player;
                }
            }

            if (curPlayer == null) {
                logger.error("curPlayer is null");
                new Exception("curPlayer is null").printStackTrace();
                return;
            }

            if (result == EGameResultStatus.CASHED_OUT) {
//                curPlayer.changeBalance(seat.getCurrentBet() / 2);
                curPlayer.changeBalance(seat.getCurrentBet().divide(BigDecimal.valueOf(2)));
//                curPlayer.setBalanceDifference(curPlayer.getBalance());
            }

            if (result == EGameResultStatus.LOST) {
                //dealer.changeAmountOfMoney(seat.getCurrentBet()); //this is just for fun
            }

            if (result == EGameResultStatus.TOO_MANY) {
                //dealer.changeAmountOfMoney(seat.getCurrentBet()); //this is just for fun
            }

            if (result == EGameResultStatus.WON) {
//                curPlayer.changeBalance(seat.getCurrentBet() * 2);
                curPlayer.changeBalance(seat.getCurrentBet().multiply(BigDecimal.valueOf(2)));

            }

            if (result == EGameResultStatus.BLACKJACK) {
//                curPlayer.changeBalance((int) (seat.getCurrentBet() * 2.5));//in general x1.5, but here is 2.5
                curPlayer.changeBalance(seat.getCurrentBet().multiply(BigDecimal.valueOf(2.5)));
            }

            if (result == EGameResultStatus.PUSHED) {
                curPlayer.changeBalance(seat.getCurrentBet());
            }
        }

/*        if (playersBroadcastCallback == null) {
            logger.error("playersBroadcastCallback is null");
            new Exception("playersBroadcastCallback is null").printStackTrace();
            return;
        }*/

        playersBroadcast();//if im not wrong - its for sending of results at the end of the game

        for (Player player : players) {
            System.err.println(player.getPlayerUUID() + " - balance - " + player.getBalance() + " delta - " + player.getBalanceDifference());
        }
    }


    public EDecision gettingDecision(Seat seat) {
        //TODO display it in the players' interface
        messageSender.broadcast(new MyPackage<>(seat, EMessageType.CURRENT_SEAT));

//        timerForDecision = new MyTimer();//TODO mb initialise not here...

/*        new Thread(() -> {
//            timer.startTimer(TIME_FOR_DECISION);
            timerService.start(TimerType.DECISION_TIME, 15, );

        }).start();*/
/*        timerService.start(TimerType.DECISION_TIME, TIME_FOR_DECISION, time -> {
            messageSender.broadcast(new MyPackage<>(time, EMessageType.TIMER));

            if (time == 0 && decisionField == null) {
                decisionField = basicDecision(seat);
            }
        });*/
        timerService.start(TimerType.DECISION_TIME, TIME_FOR_DECISION, decisionTimeObserver);

        try {//it's necessarily because of thread...
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (decisionField == null && timerService.isRunning(TimerType.DECISION_TIME)) {
            System.out.println("Decision button is empty, but timer is running yet");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        System.out.println("AFTER empty while (waiting for a decision)");

        if (decisionField != null /*&& timerForDecision.isRunning()*/) {
            System.out.println("Decision was made on time");
            return getDecisionField();
        }

        if (decisionField == null /*&& !timerForDecision.isRunning()*/) {
            EDecision decision = basicDecision(seat);
            logger.info("decisionField == null and timer is over -> basicDecision - " + decision);
            return decision;
        }

        return null;
    }

    public EDecision basicDecision(Seat seat) {
        if (seat.getMainScore() > 11) {
            return EDecision.STAND;
//            seat.setCurrentDecision(EDecision.STAND);
        } else {
            return EDecision.HIT;
//            seat.setCurrentDecision(EDecision.HIT);
        }
    }


    public boolean isAbleToSplit(Seat seat) {//idk
        return seat.getMainHand().getFirst().getCoefficient() ==
                seat.getMainHand().getLast().getCoefficient();
    }

    public boolean isFirstDecision(Seat seat) {//idk
        return seat.getLastDecision() == null;
    }

    //TODO playersBroadcast - bed violation!!!
    public void playersBroadcast() {//It's for sending to certain player his player data
        for (Player player : players) {
            messageSender.sendToClient(player.getPlayerUUID(), new MyPackage<>(player, EMessageType.CURRENT_DATA_ABOUT_PLAYER));
        }
    }
}
