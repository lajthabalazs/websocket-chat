package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.game.Game;
import ca.lajtha.websocketchat.game.VoidGame;
import ca.lajtha.websocketchat.websocket.WebSocketFrameHandler;
import ca.lajtha.websocketchat.websocket.WebSocketServer;
import com.google.inject.AbstractModule;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind ServerConfig interface to PropertiesServerConfig implementation as a singleton
        // since it loads configuration once
        bind(ServerConfig.class).to(PropertiesServerConfig.class).asEagerSingleton();

        // Bind Game interface to ChatGame implementation as a singleton
        // Replace with your own implementation by binding Game to a different class
        bind(Game.class).to(VoidGame.class).asEagerSingleton();
        
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

