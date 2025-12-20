package ca.lajtha.websocketchat.websocket;

import ca.lajtha.websocketchat.ServerConfig;
import ca.lajtha.websocketchat.game.chat.ChatGameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServerTest {

    @Mock
    private ServerConfig config;

    @Mock
    private ChatGameController chatGameController;

    @Mock
    private PlayerWebsocketConnectionManager websocketConnectionManager;

    private WebSocketServer server;

    @BeforeEach
    void setUp() {
        lenient().when(config.getPort()).thenReturn(8080);
        lenient().when(config.getWebsocketPath()).thenReturn("/websocket");
        lenient().when(config.getSocketBacklog()).thenReturn(128);
        lenient().when(config.isSocketKeepalive()).thenReturn(true);
        lenient().when(config.getHttpMaxContentLength()).thenReturn(65536);

        server = new WebSocketServer(config, websocketConnectionManager, chatGameController);
    }

    @Test
    void WebSocketServer_success() {
        // Act
        WebSocketServer server = new WebSocketServer(config, websocketConnectionManager, chatGameController);

        // Assert
        assertNotNull(server);
    }

    @Test
    void WebSocketServer_withValidDependencies() {
        // Verify that the server can be created with mocked dependencies
        assertNotNull(server);
        
        // The start() method will try to bind to a real port, so we can't easily test
        // the full startup without integration tests, but we can verify the constructor works
        assertDoesNotThrow(() -> {
            WebSocketServer testServer = new WebSocketServer(config, websocketConnectionManager, chatGameController);
            assertNotNull(testServer);
        });
    }
}

