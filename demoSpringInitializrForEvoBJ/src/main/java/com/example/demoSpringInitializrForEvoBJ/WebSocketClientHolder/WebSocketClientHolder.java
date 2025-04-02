package com.example.demoSpringInitializrForEvoBJ.WebSocketClientHolder;

import com.example.demoSpringInitializrForEvoBJ.Client;
import com.example.demoSpringInitializrForEvoBJ.Player;
//import com.example.demoSpringInitializrForEvoBJ.myPackage.ClientFinder;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketClientHolder {
    @Getter
    private final Map<String, Client> temporaryClients = new ConcurrentHashMap<>();
    @Getter
    private final Map<UUID, Client> authenticatedClients = new ConcurrentHashMap<>();

/*    @Getter
    private final ClientFinder clientFinder;

    public WebSocketClientHolder() {
        this.clientFinder = new ClientFinder(temporaryClients, authenticatedClients);
    }*/

    public void addTemporaryClient(String sessionId, Client client) {
        temporaryClients.put(sessionId, client);
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
