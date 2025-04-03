package com.example.demoSpringInitializrForEvoBJ.common;

import com.example.demoSpringInitializrForEvoBJ.websocket.message.MessageSender;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.EMessageType;
import com.example.demoSpringInitializrForEvoBJ.websocket.message.MyPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class GameExceptionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(GameExceptionProcessor.class);
    private final MessageSender sender;

    public GameExceptionProcessor(MessageSender sender) {
        this.sender = sender;
    }

    public void process(Exception e, UUID playerId) {//TODO client must handle it
        if (e instanceof GameValidationException gve) {
            sender.sendToClient(playerId, new MyPackage<>(gve.getMessage(), EMessageType.VALIDATION_ERROR));
        } else if (e instanceof GameSystemException gse) {
            sender.sendToClient(playerId, new MyPackage<>(gse.getUserDescription(), EMessageType.SYSTEM_ERROR));
            logger.error("System error [" + gse.getCode() + "]", gse);
        } else {
            String unknownCode = ErrorCodeGenerator.generateErrorId("UNKNOWN");
            logger.error("Unknown error [{}]", unknownCode, e);
            sender.sendToClient(playerId, new MyPackage<>(
                    "Something went wrong. Contact support. Error code: " + unknownCode, EMessageType.SYSTEM_ERROR));
        }
    }
}