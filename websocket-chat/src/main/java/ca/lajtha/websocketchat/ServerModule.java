package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.game.Game;
import ca.lajtha.websocketchat.game.chat.ChatGameController;
import ca.lajtha.websocketchat.server.PropertiesServerConfig;
import ca.lajtha.websocketchat.server.ServerConfig;
import ca.lajtha.websocketchat.user.InMemoryUserDatabase;
import ca.lajtha.websocketchat.user.UserDatabase;
import ca.lajtha.websocketchat.server.websocket.PlayerMessageSender;
import ca.lajtha.websocketchat.server.websocket.PlayerWebsocketConnectionManager;
import ca.lajtha.websocketchat.server.websocket.WebSocketFrameHandler;
import ca.lajtha.websocketchat.server.websocket.WebSocketServer;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind PropertiesLoader as a singleton
        bind(PropertiesLoader.class).in(Scopes.SINGLETON);
        
        // Bind UserDatabase interface to InMemoryUserDatabase implementation as a singleton
        bind(UserDatabase.class).to(InMemoryUserDatabase.class).in(Scopes.SINGLETON);
        
        // Bind ServerConfig interface to PropertiesServerConfig implementation as a singleton
        // since it loads configuration once
        bind(ServerConfig.class).to(PropertiesServerConfig.class).asEagerSingleton();

        bind(PlayerWebsocketConnectionManager.class).in(Scopes.SINGLETON);

        bind(PlayerMessageSender.class).to(PlayerWebsocketConnectionManager.class);

        bind(Game.class).to(ChatGameController.class).asEagerSingleton();
        
        // Bind WebSocketServer
        bind(WebSocketServer.class);
        
        // Bind WebSocketFrameHandler (can be instantiated per connection if needed)
        bind(WebSocketFrameHandler.class);
        
        // Note: HTTP server is now handled by Micronaut controllers
        // HttpServer and HttpRequestHandler are no longer needed
    }
}

