package com.example.demoSpringInitializrForEvoBJ;

import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;

import java.util.UUID;

public interface GameToMessageHandlerListener {
//    void onMessage(MyPackage<?> myPackage);

    void broadcast(MyPackage<?> message);
    void sendToClient(UUID playerUUID, MyPackage<?> message);
}
