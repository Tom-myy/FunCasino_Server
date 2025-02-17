package com.example.demoSpringInitializrForEvoBJ;

import com.example.demoSpringInitializrForEvoBJ.myPackage.MyPackage;

public interface GameToMessageHandlerListener {
    void onMessage(MyPackage<?> myPackage);

    void broadcast(MyPackage<?> message);
    void sendToClient(String playerUUID, MyPackage<?> message);
}
