package com.example.demoSpringInitializrForEvoBJ;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

/*@Getter
@Setter*/
public class Client {
    @Getter
    private WebSocketSession session = null;
    private ConnectionStatus connectionStatus = null;
    @Getter
    @Setter
    private Player player = null;

    public Client(WebSocketSession session) {
        this.session = session;
        connectionStatus = ConnectionStatus.ALMOST_CONNECTED;
    }

    public void setConnectionStatusToConnect() {
        connectionStatus = ConnectionStatus.CONNECTED;
    }
}
