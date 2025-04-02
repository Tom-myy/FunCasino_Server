package com.example.demoSpringInitializrForEvoBJ.Message;

import com.example.demoSpringInitializrForEvoBJ.Client;
import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;

import java.util.UUID;

public interface IMessageSender {
    void sendToClient(Client client, MyPackage<?> message);
    void sendToClient(UUID playerUUID, MyPackage<?> message);
    void broadcast(MyPackage<?> message);
}
