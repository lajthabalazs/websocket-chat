package ca.lajtha;

import com.google.inject.AbstractModule;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind ServerConfig as a singleton since it loads configuration once
        bind(ServerConfig.class).asEagerSingleton();
        
        // Bind WebSocketServer
        bind(WebSocketServer.class);
        
        // Bind WebSocketFrameHandler (can be instantiated per connection if needed)
        bind(WebSocketFrameHandler.class);
    }
}

