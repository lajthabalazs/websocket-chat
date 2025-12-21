package ca.lajtha.websocketchat.server.websocket;

public interface MessageSender {
        void sendMessage(String socketId, String message);
}
