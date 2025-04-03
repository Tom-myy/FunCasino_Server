package com.example.demoSpringInitializrForEvoBJ.game.service;

import com.example.demoSpringInitializrForEvoBJ.player.PlayerRegistry;
import com.example.demoSpringInitializrForEvoBJ.timer.TimerType;
import com.example.demoSpringInitializrForEvoBJ.player.dto.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.game.EDecision;
import com.example.demoSpringInitializrForEvoBJ.game.model.Game;
import com.example.demoSpringInitializrForEvoBJ.game.model.Seat;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import com.example.demoSpringInitializrForEvoBJ.player.model.Player;
import com.example.demoSpringInitializrForEvoBJ.player.service.EvoUserService;
import com.example.demoSpringInitializrForEvoBJ.timer.BettingTimeObserver;
import com.example.demoSpringInitializrForEvoBJ.timer.DecisionTimeObserver;
import com.example.demoSpringInitializrForEvoBJ.timer.TimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final BettingTimeObserver bettingTimeObserver;
    private final EvoUserService evoUserService;
    private final TableService tableService;
    private final TimerService timerService;
    private final PlayerRegistry playerRegistry;
    private final MessageSender messageSender;
    private final Game game;

    public GameService(BettingTimeObserver bettingTimeObserver, DecisionTimeObserver decisionTimeObserver, EvoUserService evoUserService, TableService tableService, TimerService timerService, PlayerRegistry playerRegistry, MessageSender messageSender) {
        this.bettingTimeObserver = bettingTimeObserver;
        this.evoUserService = evoUserService;
        this.tableService = tableService;
        this.timerService = timerService;
        this.playerRegistry = playerRegistry;
        this.messageSender = messageSender;
        this.game = new Game(tableService.getTable(), messageSender, timerService, playerRegistry, decisionTimeObserver);
    }

    public void startGame() {
        game.startGame();
    }

    public boolean isGameRunning() {
        return game.isGameRunning();
    }

    /*    public void tryStartBettingTime() {
            if (!timerService.isRunning(TimerType.BETTING_TIME)) {
                if (tableService.isTableReadyToStartGame()) {
                    timerService.start(TimerType.BETTING_TIME, 10, time -> {
                        if (time == 0 && tableService.isTableReadyToStartGame()) {
                            startGame();
                        } else {
                            messageSender.broadcast(new MyPackage<>(time, EMessageType.TIMER));
                        }
                    });
                }
            }
        }*/
    public void tryStartBettingTime() {
        if (!timerService.isRunning(TimerType.BETTING_TIME)) {
            if (tableService.isTableReadyToStartGame()) {
                bettingTimeObserver.setOnTimeout(() -> {
                    if (tableService.isTableReadyToStartGame() && !game.isGameRunning()) {
//                        startGame();
                        startGameAsync();
                    }
                });
                timerService.start(TimerType.BETTING_TIME, 10, bettingTimeObserver);
            }
        }
    }

    public void processRequestToStartGame(UUID clientUUID) {
        for (Player p : playerRegistry.getPlayers()) {
            if (clientUUID.equals(p.getPlayerUUID())) {
                p.setWantsToStartGame(true);//TODO think about when player wanted to start game (clicked button)
                // and then he left game - I need to uncheck his wish to start game
                break;
            }
        }

        List<Player> tmpPlayersWithBet = new ArrayList<>();

        for (Seat s : tableService.getCalculatedGameSeats()) {
            for (Player p : playerRegistry.getPlayers()) {
                if (s.getPlayerUUID().equals(p.getPlayerUUID())) {
                    tmpPlayersWithBet.add(p);
                    break;
                }
            }
        }

        boolean allPlayersWantsToStartGame = true;
        for (Player p : tmpPlayersWithBet) {
            if (!p.isWantsToStartGame()) {
                allPlayersWantsToStartGame = false;
                break;
            }
        }

        if (allPlayersWantsToStartGame) {
            startGameAsync();
        }
    }

    private void handleAfterGame(List<Player> players) {
        if (players == null) {
            logger.error("Game result is null");
            return;
        }

        List<Player> updated = evoUserService.updateUsersAfterGame(players);
        if (updated == null) {
            logger.error("Update failed");
            return;
        }

        List<EvoUserDTO> dtos = evoUserService.getUpdatedUsers(updated);
        if (dtos == null) {
            logger.error("DTO fetch failed");
            return;
        }

        for (EvoUserDTO dto : dtos) {
            messageSender.sendToClient(dto.getPlayerUUID(), new MyPackage<>(dto, EMessageType.FRESH_DTOS));
        }
    }

    private void startGameAsync() {
        executor.submit(() -> {
            try {
                System.err.println("startGameAsync - before game");
                List<Player> result = game.startGame();
                System.err.println("startGameAsync - after game and before 'handleAfterGame'");
                handleAfterGame(result);
                System.err.println("startGameAsync - after 'handleAfterGame'");
            } catch (Exception e) {
                logger.error("Game failed", e);
            }
        });
    }

    public void setDecisionField(EDecision decision) {
        game.setDecisionField(decision);
    }
}