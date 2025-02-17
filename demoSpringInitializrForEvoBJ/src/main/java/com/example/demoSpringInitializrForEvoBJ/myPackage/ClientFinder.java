package com.example.demoSpringInitializrForEvoBJ.myPackage;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public class ClientFinder {
    private Map<String, WebSocketSession> clients;

    public ClientFinder(Map<String, WebSocketSession> clients) {
        this.clients = clients;
    }

    public String findUUIDByClient(WebSocketSession client) {
        for (Map.Entry<String, WebSocketSession> entry: clients.entrySet()){
            if (entry.getValue().equals(client)){
                return entry.getKey();
            }
        }
        return null;
    }

    public WebSocketSession findClientByUUID(String clientUUID) {
        return clients.get(clientUUID);
    }
}
