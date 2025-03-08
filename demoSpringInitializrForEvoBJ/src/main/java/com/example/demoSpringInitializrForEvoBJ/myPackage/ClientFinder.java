package com.example.demoSpringInitializrForEvoBJ.myPackage;

import com.example.demoSpringInitializrForEvoBJ.Client;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

public class ClientFinder {
//    private Map<String, Client> notReadyClients;
    private Map<String, Client> clients;

    public ClientFinder(/*Map<String, Client> notReadyClients, */Map<String, Client> clients) {
//        this.notReadyClients = notReadyClients;
        this.clients = clients;
    }

    public String findUUIDBySession(WebSocketSession session) {
        for (Map.Entry<String, Client> entry: clients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getKey();
            }
        }
        return null;
    }

    public Client findClientByUUID(String clientUUID) {
        return clients.get(clientUUID);
    }

    public Client findClientBySession(WebSocketSession session) {
        for (Map.Entry<String, Client> entry: clients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getValue();
            }
        }
        return null;
    }

//    public String findNotReadyUUIDBySession(WebSocketSession session) {
//        for (Map.Entry<String, Client> entry: notReadyClients.entrySet()){
//            if (entry.getValue().getSession().equals(session)){
//                return entry.getKey();
//            }
//        }
//        return null;
//    }
//
//    public Client findNotReadyClientByUUID(String clientUUID) {
//        return notReadyClients.get(clientUUID);//TODO mb i need get this client, return him and delete from collection?
//    }


}
