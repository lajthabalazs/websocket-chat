package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.game.Game;
import ca.lajtha.websocketchat.game.VoidGame;
import ca.lajtha.websocketchat.game.chat.ChatGameController;
import ca.lajtha.websocketchat.websocket.PlayerConnection;
import ca.lajtha.websocketchat.websocket.PlayerWebsocketConnectionManager;
import ca.lajtha.websocketchat.websocket.WebSocketFrameHandler;
import ca.lajtha.websocketchat.websocket.WebSocketServer;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind ServerConfig interface to PropertiesServerConfig implementation as a singleton
        // since it loads configuration once
        bind(ServerConfig.class).to(PropertiesServerConfig.class).asEagerSingleton();

        bind(PlayerWebsocketConnectionManager.class).in(Scopes.SINGLETON);

        bind(PlayerConnection.class).to(PlayerWebsocketConnectionManager.class);

        bind(Game.class).to(ChatGameController.class).asEagerSingleton();
        
        // Bind WebSocketServer
        bind(WebSocketServer.class);
        
        // Bind WebSocketFrameHandler (can be instantiated per connection if needed)
        bind(WebSocketFrameHandler.class);
        
        // Bind HttpServer
        bind(ca.lajtha.websocketchat.http.HttpServer.class);
        
        // Bind HttpRequestHandler
        bind(ca.lajtha.websocketchat.http.HttpRequestHandler.class);
    }
}

