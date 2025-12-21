package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.game.Game;
import ca.lajtha.websocketchat.game.chat.ChatGame;
import ca.lajtha.websocketchat.server.PropertiesServerConfig;
import ca.lajtha.websocketchat.server.ServerConfig;
import ca.lajtha.websocketchat.user.InMemoryUserDatabase;
import ca.lajtha.websocketchat.user.UserDatabase;
import ca.lajtha.websocketchat.game.PlayerMessageSender;
import ca.lajtha.websocketchat.game.ConnectionManager;
import ca.lajtha.websocketchat.game.GameManager;
import ca.lajtha.websocketchat.server.websocket.WebSocketFrameHandler;
import ca.lajtha.websocketchat.server.websocket.WebSocketServer;
import ca.lajtha.websocketchat.server.websocket.WebsocketManager;
import ca.lajtha.websocketchat.server.websocket.WebsocketManagerImpl;
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

        bind(ConnectionManager.class).in(Scopes.SINGLETON);

        bind(PlayerMessageSender.class).to(ConnectionManager.class);

        bind(Game.class).to(ChatGame.class).asEagerSingleton();
        
        // Bind GameManager as a singleton
        bind(GameManager.class).in(Scopes.SINGLETON);
        
        // Bind WebsocketManager to WebsocketManagerImpl
        bind(WebsocketManager.class).to(WebsocketManagerImpl.class).in(Scopes.SINGLETON);
        
        // Bind WebSocketServer
        bind(WebSocketServer.class);
        
        // Bind WebSocketFrameHandler (can be instantiated per connection if needed)
        bind(WebSocketFrameHandler.class);
        
        // Note: HTTP server is now handled by Micronaut controllers
        // HttpServer and HttpRequestHandler are no longer needed
    }
}

