package com.example.demoSpringInitializrForEvoBJ.websocket.handler;

import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);
    private final AuthHandler authHandler;
    private final SeatHandler seatHandler;
    private final GameHandler gameHandler;

    public MessageDispatcher(AuthHandler authHandler, SeatHandler seatHandler, GameHandler gameHandler) {
        this.authHandler = authHandler;
        this.seatHandler = seatHandler;
        this.gameHandler = gameHandler;
    }

    public void dispatch(MyPackage<?> myPackage, WebSocketSession session) {
        EMessageType messageType = myPackage.getMessageType();

        switch (messageType) {
            case AUTHORIZATION -> authHandler.handleAuthorization(myPackage, session);//TODO code refactoring
            case MAIN_FORM_INITIALIZATION -> authHandler.handleInitializationOfMainForm(myPackage, session);//TODO code refactoring

            case TAKE_SEAT -> seatHandler.handleTakeSeat(myPackage, session);
            case LEAVE_SEAT -> seatHandler.handleLeaveSeat(myPackage, session);
            case UPDATE_SEAT_BET -> seatHandler.handleUpdateSeatBet(myPackage, session);

            case REQUEST_TO_START_GAME -> gameHandler.handleRequestToStartGame(myPackage, session);
            case GAME_DECISION -> gameHandler.handleGameDecision(myPackage, session);

            default -> {
                logger.warn("Server got a unknown MESSAGE type!");
            }
        }
    }
}
