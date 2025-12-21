package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.server.PropertiesServerConfig;
import ca.lajtha.websocketchat.server.ServerConfig;
import ca.lajtha.websocketchat.server.websocket.*;
import ca.lajtha.websocketchat.user.InMemoryUserDatabase;
import ca.lajtha.websocketchat.user.TokenManager;
import ca.lajtha.websocketchat.user.UserDatabase;
import ca.lajtha.websocketchat.game.ConnectionManager;
import ca.lajtha.websocketchat.game.GameManager;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {

        // Bind PropertiesLoader as a singleton
        bind(PropertiesLoader.class).in(Scopes.SINGLETON);

        // Bind TokenManager as a singleton
        bind(TokenManager.class).in(Scopes.SINGLETON);

        // Bind UserDatabase interface to InMemoryUserDatabase implementation as a singleton
        bind(UserDatabase.class).to(InMemoryUserDatabase.class).in(Scopes.SINGLETON);

        // Bind ServerConfig interface to PropertiesServerConfig implementation as a singleton
        // since it loads configuration once
        bind(ServerConfig.class).to(PropertiesServerConfig.class).asEagerSingleton();

        WebsocketManagerImpl websocketManager = new WebsocketManagerImpl();
        ConnectionManager connectionManager = new ConnectionManager(websocketManager, getProvider(TokenManager.class).get());
        GameManager gameManager = new GameManager(connectionManager);
        connectionManager.setGame(gameManager);

        // Register ConnectionManager as a message listener
        websocketManager.addMessageListener(connectionManager);

        // Bind WebsocketManager to WebsocketManagerImpl
        bind(WebsocketManager.class).toInstance(websocketManager);

        // Bind WebSocketServer
        bind(WebSocketServer.class);

    }
}

