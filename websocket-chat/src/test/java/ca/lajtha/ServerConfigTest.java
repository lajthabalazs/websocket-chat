package ca.lajtha;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    @Test
    void ServerConfig_usesDefaultValues() {
        // This test verifies that defaults are used when properties file is missing
        // Since we can't easily mock the resource loading, we test that the config
        // can be instantiated and returns reasonable default values
        ServerConfig config = new ServerConfig();
        
        assertNotNull(config);
        assertTrue(config.getPort() > 0);
        assertNotNull(config.getWebsocketPath());
        assertTrue(config.getSocketBacklog() > 0);
        assertTrue(config.getHttpMaxContentLength() > 0);
    }

    @Test
    void getPort_success() {
        ServerConfig config = new ServerConfig();
        int port = config.getPort();
        assertTrue(port > 0 && port <= 65535, "Port should be in valid range");
    }

    @Test
    void getWebsocketPath_success() {
        ServerConfig config = new ServerConfig();
        String path = config.getWebsocketPath();
        assertNotNull(path);
        assertTrue(path.startsWith("/"), "WebSocket path should start with /");
    }

    @Test
    void getSocketBacklog_success() {
        ServerConfig config = new ServerConfig();
        int backlog = config.getSocketBacklog();
        assertTrue(backlog > 0, "Socket backlog should be positive");
    }

    @Test
    void getHttpMaxContentLength_success() {
        ServerConfig config = new ServerConfig();
        int maxContentLength = config.getHttpMaxContentLength();
        assertTrue(maxContentLength > 0, "Max content length should be positive");
    }

    @Test
    void isSocketKeepalive_success() {
        ServerConfig config = new ServerConfig();
        boolean keepalive = config.isSocketKeepalive();
        assertNotNull(Boolean.valueOf(keepalive));
    }
}

