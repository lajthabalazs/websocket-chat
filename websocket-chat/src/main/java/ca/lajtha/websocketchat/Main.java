package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.http.HttpServer;
import ca.lajtha.websocketchat.websocket.WebSocketServer;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Injector injector = Guice.createInjector(new ServerModule());
        
        // Start WebSocket server in a separate thread
        WebSocketServer webSocketServer = injector.getInstance(WebSocketServer.class);
        Thread webSocketThread = new Thread(() -> {
            try {
                webSocketServer.start();
            } catch (InterruptedException e) {
                System.err.println("WebSocket server interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
        webSocketThread.setName("WebSocketServer");
        webSocketThread.start();
        
        // Start HTTP server in the main thread
        HttpServer httpServer = injector.getInstance(HttpServer.class);
        httpServer.start();
    }
}
