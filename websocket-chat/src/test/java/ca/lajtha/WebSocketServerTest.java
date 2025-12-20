package ca.lajtha;

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
    private WebSocketFrameHandler frameHandler;

    private WebSocketServer server;

    @BeforeEach
    void setUp() {
        lenient().when(config.getPort()).thenReturn(8080);
        lenient().when(config.getWebsocketPath()).thenReturn("/websocket");
        lenient().when(config.getSocketBacklog()).thenReturn(128);
        lenient().when(config.isSocketKeepalive()).thenReturn(true);
        lenient().when(config.getHttpMaxContentLength()).thenReturn(65536);

        server = new WebSocketServer(config, frameHandler);
    }

    @Test
    void WebSocketServer_success() {
        // Act
        WebSocketServer server = new WebSocketServer(config, frameHandler);

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
            WebSocketServer testServer = new WebSocketServer(config, frameHandler);
            assertNotNull(testServer);
        });
    }
}

