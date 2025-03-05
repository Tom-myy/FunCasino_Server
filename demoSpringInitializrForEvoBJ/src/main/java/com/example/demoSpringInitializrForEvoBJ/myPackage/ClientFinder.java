package com.example.demoSpringInitializrForEvoBJ.myPackage;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public class ClientFinder {
    private Map<String, WebSocketSession> notReadyClients;
    private Map<String, WebSocketSession> clients;

    public ClientFinder(Map<String, WebSocketSession> notReadyClients, Map<String, WebSocketSession> clients) {
        this.notReadyClients = notReadyClients;
        this.clients = clients;
    }

    public String findReadyUUIDByClient(WebSocketSession client) {
        for (Map.Entry<String, WebSocketSession> entry: clients.entrySet()){
            if (entry.getValue().equals(client)){
                return entry.getKey();
            }
        }
        return null;
    }

    public WebSocketSession findReadyClientByUUID(String clientUUID) {
        return clients.get(clientUUID);
    }

    public String findNotReadyUUIDByClient(WebSocketSession client) {
        for (Map.Entry<String, WebSocketSession> entry: notReadyClients.entrySet()){
            if (entry.getValue().equals(client)){
                return entry.getKey();
            }
        }
        return null;
    }

    public WebSocketSession findNotReadyClientByUUID(String clientUUID) {
        return notReadyClients.get(clientUUID);
    }


}
