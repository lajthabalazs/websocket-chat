package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.server.websocket.*;
import ca.lajtha.websocketchat.user.InMemoryUserDatabase;
import ca.lajtha.websocketchat.user.UserDatabase;
import ca.lajtha.websocketchat.game.ConnectionManager;
import ca.lajtha.websocketchat.game.GameManager;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * Factory for creating WebSocket server components as Micronaut beans.
 * Only creates beans that need special setup (interfaces, circular dependencies, etc.).
 * Other beans are auto-discovered via @Singleton annotation.
 */
@Factory
public class ServerModule {
    
    @Bean
    @Singleton
    public UserDatabase userDatabase() {
        return new InMemoryUserDatabase();
    }
    
    @Bean
    @Singleton
    public WebsocketManagerImpl websocketManagerImpl() {
        return new WebsocketManagerImpl();
    }
    
    @Bean
    @Singleton
    public ConnectionManager connectionManager(WebsocketManagerImpl websocketManagerImpl) {
        ConnectionManager connectionManager = new ConnectionManager(websocketManagerImpl);
        // Register ConnectionManager as a message listener
        websocketManagerImpl.addMessageListener(connectionManager);
        return connectionManager;
    }
    
    @Bean
    @Singleton
    public GameManager gameManager(ConnectionManager connectionManager) {
        GameManager gameManager = new GameManager(connectionManager);
        // Wire up the circular dependency: ConnectionManager needs GameManager
        connectionManager.setGame(gameManager);
        return gameManager;
    }
    
    @Bean
    @Singleton
    public WebsocketManager websocketManager(WebsocketManagerImpl websocketManagerImpl) {
        return websocketManagerImpl;
    }
}

