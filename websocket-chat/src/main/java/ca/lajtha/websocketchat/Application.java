package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.server.ServerConfig;
import ca.lajtha.websocketchat.server.websocket.WebSocketServer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;

import java.util.Map;

public class Application {
    
    private static ApplicationContext micronautContext;
    
    public static void main(String[] args) {
        // Create Guice injector for WebSocket server
        Injector injector = Guice.createInjector(new ServerModule());
        // Get server config for HTTP port
        ServerConfig config = injector.getInstance(ServerConfig.class);

        startWebsocketServer(injector);
        startWebServer(args, config);
        
        // Keep the application running
        try {
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down...");
                if (micronautContext != null) {
                    micronautContext.close();
                }
            }));
            
            // Wait indefinitely
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Application interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static void startWebServer(String[] args, ServerConfig config) {
        // Start Micronaut HTTP server
        micronautContext = Micronaut.build(args)
                .properties(Map.of(
                    "micronaut.server.port", String.valueOf(config.getHttpPort())
                ))
                .start();

        System.out.println("HTTP server started on port " + config.getHttpPort());
        System.out.println("Connect to: http://localhost:" + config.getHttpPort());
    }

    private static void startWebsocketServer(Injector injector) {
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
    }
}

