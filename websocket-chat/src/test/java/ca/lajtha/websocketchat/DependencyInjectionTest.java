package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.game.ConnectionManager;
import ca.lajtha.websocketchat.game.GameManager;
import ca.lajtha.websocketchat.server.ServerConfig;
import ca.lajtha.websocketchat.server.websocket.WebSocketServer;
import ca.lajtha.websocketchat.server.websocket.WebsocketManager;
import ca.lajtha.websocketchat.user.TokenManager;
import ca.lajtha.websocketchat.user.UserDatabase;
import ca.lajtha.websocketchat.user.UserManager;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that all classes are properly initialized via dependency injection.
 * 
 * This test ensures that Micronaut's DI system creates all beans and wires dependencies correctly.
 * The primary verification is that the ApplicationContext starts successfully, which means:
 * - All beans are discovered and created
 * - All dependencies are resolved
 * - Circular dependencies are handled
 * - All @Inject annotations are processed
 * 
 * If the context fails to start, it indicates missing dependencies, unresolvable circular dependencies,
 * or other DI configuration issues.
 */
class DependencyInjectionTest {

    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        // Start Micronaut application context to initialize all beans
        // If this succeeds, it means all dependencies are resolved and beans are created
        applicationContext = Micronaut.build(new String[0]).start();
    }

    @AfterEach
    void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    void applicationContextStartsSuccessfully() {
        // The fact that setUp() succeeded means all beans were initialized
        // If there were missing dependencies or circular dependencies that couldn't be resolved,
        // the context would fail to start
        assertNotNull(applicationContext, "Application context should be initialized");
        assertTrue(applicationContext.isRunning(), "Application context should be running");

    }

    @Test
    void coreBeansAreInitialized() {
        // Verify that core beans can be retrieved, confirming they were initialized
        // We use findBean() which returns Optional to handle cases where there might be
        // multiple bean definitions (e.g., auto-discovered + factory-created)
        
        assertTrue(applicationContext.findBean(PropertiesLoader.class).isPresent(),
            "PropertiesLoader should be initialized");
        assertTrue(applicationContext.findBean(ServerConfig.class).isPresent(),
            "ServerConfig should be initialized");
        assertTrue(applicationContext.findBean(TokenManager.class).isPresent(),
            "TokenManager should be initialized");
        assertTrue(applicationContext.findBean(UserDatabase.class).isPresent(),
            "UserDatabase should be initialized");
        assertTrue(applicationContext.findBean(UserManager.class).isPresent(),
            "UserManager should be initialized");
    }

    @Test
    void websocketBeansAreInitialized() {
        // Verify that WebSocket-related beans are initialized
        assertTrue(applicationContext.findBean(WebsocketManager.class).isPresent(),
            "WebsocketManager should be initialized");
        assertTrue(applicationContext.findBean(ConnectionManager.class).isPresent(),
            "ConnectionManager should be initialized");
        assertTrue(applicationContext.findBean(GameManager.class).isPresent(),
            "GameManager should be initialized");
        assertTrue(applicationContext.findBean(WebSocketServer.class).isPresent(),
            "WebSocketServer should be initialized");
    }

    @Test
    void dependenciesAreInjected() {
        // Verify that dependencies are properly injected by checking that beans
        // can be retrieved and have their dependencies satisfied
        
        // If these beans exist, their dependencies were injected
        TokenManager tokenManager = applicationContext.findBean(TokenManager.class).orElse(null);
        assertNotNull(tokenManager, "TokenManager should be created with PropertiesLoader injected");
        
        ServerConfig serverConfig = applicationContext.findBean(ServerConfig.class).orElse(null);
        assertNotNull(serverConfig, "ServerConfig should be created with PropertiesLoader injected");
        assertTrue(serverConfig.getPort() > 0, "ServerConfig should have valid configuration");
        
        UserManager userManager = applicationContext.findBean(UserManager.class).orElse(null);
        assertNotNull(userManager, "UserManager should be created with UserDatabase and PropertiesLoader injected");
        
        WebSocketServer webSocketServer = applicationContext.findBean(WebSocketServer.class).orElse(null);
        assertNotNull(webSocketServer, "WebSocketServer should be created with all dependencies injected");
    }

    @Test
    void circularDependencyIsResolved() {
        // Verify that the circular dependency between ConnectionManager and GameManager is resolved
        // The fact that the application context started successfully means the circular dependency was resolved
        
        ConnectionManager connectionManager = applicationContext.findBean(ConnectionManager.class).orElse(null);
        GameManager gameManager = applicationContext.findBean(GameManager.class).orElse(null);
        
        assertNotNull(connectionManager, "ConnectionManager should be created");
        assertNotNull(gameManager, "GameManager should be created");
        
        // Both beans exist, which means the circular dependency was resolved
        // The factory method in ServerModule handles this by:
        // 1. Creating ConnectionManager first
        // 2. Creating GameManager with ConnectionManager
        // 3. Wiring them together via connectionManager.setGame(gameManager)
    }

    @Test
    void singletonBeansAreSameInstance() {
        // Verify that @Singleton beans return the same instance when retrieved multiple times
        // This confirms that the singleton scope is working correctly
        
        PropertiesLoader loader1 = applicationContext.findBean(PropertiesLoader.class).orElse(null);
        PropertiesLoader loader2 = applicationContext.findBean(PropertiesLoader.class).orElse(null);
        assertNotNull(loader1, "PropertiesLoader should exist");
        assertSame(loader1, loader2, "PropertiesLoader should be a singleton");
        
        TokenManager tokenManager1 = applicationContext.findBean(TokenManager.class).orElse(null);
        TokenManager tokenManager2 = applicationContext.findBean(TokenManager.class).orElse(null);
        assertNotNull(tokenManager1, "TokenManager should exist");
        assertSame(tokenManager1, tokenManager2, "TokenManager should be a singleton");
        
        ServerConfig config1 = applicationContext.findBean(ServerConfig.class).orElse(null);
        ServerConfig config2 = applicationContext.findBean(ServerConfig.class).orElse(null);
        assertNotNull(config1, "ServerConfig should exist");
        assertSame(config1, config2, "ServerConfig should be a singleton");
    }
}
