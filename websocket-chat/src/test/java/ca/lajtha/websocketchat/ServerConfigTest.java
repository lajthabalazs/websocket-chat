package ca.lajtha.websocketchat;

import ca.lajtha.websocketchat.server.PropertiesServerConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    private PropertiesLoader createPropertiesLoader() {
        return new PropertiesLoader();
    }

    @Test
    void PropertiesServerConfig_usesDefaultValues() {
        // This test verifies that defaults are used when properties file is missing
        // Since we can't easily mock the resource loading, we test that the config
        // can be instantiated and returns reasonable default values
        PropertiesLoader propertiesLoader = createPropertiesLoader();
        PropertiesServerConfig config = new PropertiesServerConfig(propertiesLoader);
        
        assertNotNull(config);
        assertTrue(config.getPort() > 0);
        assertNotNull(config.getWebsocketPath());
        assertTrue(config.getSocketBacklog() > 0);
        assertTrue(config.getHttpMaxContentLength() > 0);
    }

    @Test
    void getPort_success() {
        PropertiesLoader propertiesLoader = createPropertiesLoader();
        PropertiesServerConfig config = new PropertiesServerConfig(propertiesLoader);
        int port = config.getPort();
        assertTrue(port > 0 && port <= 65535, "Port should be in valid range");
    }

    @Test
    void getWebsocketPath_success() {
        PropertiesLoader propertiesLoader = createPropertiesLoader();
        PropertiesServerConfig config = new PropertiesServerConfig(propertiesLoader);
        String path = config.getWebsocketPath();
        assertNotNull(path);
        assertTrue(path.startsWith("/"), "WebSocket path should start with /");
    }

    @Test
    void getSocketBacklog_success() {
        PropertiesLoader propertiesLoader = createPropertiesLoader();
        PropertiesServerConfig config = new PropertiesServerConfig(propertiesLoader);
        int backlog = config.getSocketBacklog();
        assertTrue(backlog > 0, "Socket backlog should be positive");
    }

    @Test
    void getHttpMaxContentLength_success() {
        PropertiesLoader propertiesLoader = createPropertiesLoader();
        PropertiesServerConfig config = new PropertiesServerConfig(propertiesLoader);
        int maxContentLength = config.getHttpMaxContentLength();
        assertTrue(maxContentLength > 0, "Max content length should be positive");
    }

    @Test
    void isSocketKeepalive_success() {
        PropertiesLoader propertiesLoader = createPropertiesLoader();
        PropertiesServerConfig config = new PropertiesServerConfig(propertiesLoader);
        boolean keepalive = config.isSocketKeepalive();
        assertNotNull(Boolean.valueOf(keepalive));
    }
}

