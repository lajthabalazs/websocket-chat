package ca.lajtha;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerModuleTest {

    @Test
    void configure_success() {
        // Act
        Injector injector = Guice.createInjector(new ServerModule());

        // Assert - verify that all bindings are configured correctly
        ServerConfig config = injector.getInstance(ServerConfig.class);
        assertNotNull(config);
        
        WebSocketServer server = injector.getInstance(WebSocketServer.class);
        assertNotNull(server);
        
        WebSocketFrameHandler handler = injector.getInstance(WebSocketFrameHandler.class);
        assertNotNull(handler);
    }

    @Test
    void configure_ServerConfigIsSingleton() {
        // Act
        Injector injector = Guice.createInjector(new ServerModule());

        // Assert - verify that ServerConfig is a singleton
        ServerConfig config1 = injector.getInstance(ServerConfig.class);
        ServerConfig config2 = injector.getInstance(ServerConfig.class);
        
        assertSame(config1, config2, "ServerConfig should be a singleton");
    }

    @Test
    void configure_dependencyInjection() {
        // Act
        Injector injector = Guice.createInjector(new ServerModule());
        WebSocketServer server = injector.getInstance(WebSocketServer.class);

        // Assert - verify that dependencies are injected
        assertNotNull(server);
        
        // The server should have its dependencies injected via constructor
        // We can't directly verify this without reflection, but if it's created
        // successfully, the injection worked
    }
}

