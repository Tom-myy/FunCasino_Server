package com.example.demoSpringInitializrForEvoBJ.myPackage;

import com.example.demoSpringInitializrForEvoBJ.Client;
import com.example.demoSpringInitializrForEvoBJ.Player;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFinder {
//    private Map<String, Client> notReadyClients;

    private Map<String, Client> temporaryClients = new ConcurrentHashMap<>();
    private Map<UUID, Client> authenticatedClients = new ConcurrentHashMap<>();
    private List<Player> players;

    public ClientFinder(/*Map<String, Client> notReadyClients, */Map<String, Client> temporaryClients, Map<UUID, Client> authenticatedClients, List<Player> players) {
//        this.notReadyClients = notReadyClients;
        this.temporaryClients = temporaryClients;
        this.authenticatedClients = authenticatedClients;
        this.players = players;
    }

    public UUID findAuthUUIDBySession(WebSocketSession session) {
        for (Map.Entry<UUID, Client> entry: authenticatedClients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getKey();
            }
        }
        return null;
    }

    public String findTempUUIDBySession(WebSocketSession session) {
        for (Map.Entry<String, Client> entry: temporaryClients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getKey();
            }
        }
        return null;
    }

    public Client findAuthClientByUUID(UUID clientUUID) {
        return authenticatedClients.get(clientUUID);
    }

    public Client findTempClientByUUID(String clientUUID) {
        return temporaryClients.get(clientUUID);
    }

    public Player findPlayerByUUID(UUID clientUUID) {
        for(Player p: players){
            if(p.getPlayerUUID().equals(clientUUID)){
                return p;
            }
        }
        return null;
    }

    public Client findAuthClientBySession(WebSocketSession session) {
        for (Map.Entry<UUID, Client> entry: authenticatedClients.entrySet()){
            if (entry.getValue().getSession().equals(session)){
                return entry.getValue();
            }
        }
        return null;
    }

    public Client findTempClientBySession(WebSocketSession session) {
        for (Map.Entry<String, Client> entry: temporaryClients.entrySet()){
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
