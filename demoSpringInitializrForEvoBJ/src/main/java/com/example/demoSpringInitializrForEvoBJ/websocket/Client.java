package com.example.demoSpringInitializrForEvoBJ.websocket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

/*@Getter
@Setter*/
public class Client {
    @Getter
    private WebSocketSession session = null;
    @Getter
    private ConnectionStatus connectionStatus = null;
    @Getter
    @Setter
//    private Player player = null;
    private UUID playerUUID = null;

    @Getter
    @Setter
    private boolean isReadyToGetMessages = false;

    public Client(WebSocketSession session) {
        this.session = session;
        connectionStatus = ConnectionStatus.ALMOST_CONNECTED;
    }

    public void setConnectionStatusToConnect() {
        connectionStatus = ConnectionStatus.CONNECTED;
    }

    public void setConnectionStatusToDisconnect() {
        connectionStatus = ConnectionStatus.DISCONNECTED;
    }
}
