package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.server.ServerConfig;
import ca.lajtha.websocketchat.server.websocket.WebSocketServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    private static ApplicationContext micronautContext;
    
    public static void main(String[] args) {
        // Start Micronaut application context (this will create all beans)
        // First, create a context to get ServerConfig for HTTP port
        ApplicationContext tempContext = Micronaut.build(args).start();
        ServerConfig config = tempContext.getBean(ServerConfig.class);
        tempContext.close();
        
        // Now start the full Micronaut HTTP server with the correct port
        micronautContext = Micronaut.build(args)
                .properties(Map.of(
                    "micronaut.server.port", String.valueOf(config.getHttpPort())
                ))
                .start();
        
        // Get WebSocket server from Micronaut context
        WebSocketServer webSocketServer = micronautContext.getBean(WebSocketServer.class);
        startWebsocketServer(webSocketServer);
        
        // Keep the application running
        try {
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                if (micronautContext != null) {
                    micronautContext.close();
                }
            }));
            
            // Wait indefinitely
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.error("Application interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static void startWebsocketServer(WebSocketServer webSocketServer) {
        // Start WebSocket server in a separate thread
        Thread webSocketThread = new Thread(() -> {
            try {
                webSocketServer.start();
            } catch (InterruptedException e) {
                logger.error("WebSocket server interrupted", e);
                Thread.currentThread().interrupt();
            }
        });
        webSocketThread.setName("WebSocketServer");
        webSocketThread.start();
    }
}

