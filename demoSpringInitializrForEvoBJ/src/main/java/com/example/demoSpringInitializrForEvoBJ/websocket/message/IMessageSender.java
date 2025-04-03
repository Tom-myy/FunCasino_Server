package com.example.demoSpringInitializrForEvoBJ.websocket.message;

import com.example.demoSpringInitializrForEvoBJ.websocket.Client;

import java.util.UUID;

public interface IMessageSender {
    void sendToClient(Client client, MyPackage<?> message);
    void sendToClient(UUID playerUUID, MyPackage<?> message);
    void broadcast(MyPackage<?> message);
}
