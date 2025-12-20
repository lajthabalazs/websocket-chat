package ca.lajtha.websocketchat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesServerConfig implements ServerConfig {
    private final int port;
    private final String websocketPath;
    private final int socketBacklog;
    private final boolean socketKeepalive;
    private final int httpMaxContentLength;

    public PropertiesServerConfig() {
        Properties props = loadProperties();
        this.port = Integer.parseInt(props.getProperty("server.port", "8080"));
        this.websocketPath = props.getProperty("websocket.path", "/websocket");
        this.socketBacklog = Integer.parseInt(props.getProperty("socket.backlog", "128"));
        this.socketKeepalive = Boolean.parseBoolean(props.getProperty("socket.keepalive", "true"));
        this.httpMaxContentLength = Integer.parseInt(props.getProperty("http.maxContentLength", "65536"));
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input == null) {
                System.err.println("Warning: server.properties not found, using default values");
                return props;
            }
            props.load(input);
        } catch (IOException e) {
            System.err.println("Warning: Error loading server.properties, using default values: " + e.getMessage());
        }
        return props;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getWebsocketPath() {
        return websocketPath;
    }

    @Override
    public int getSocketBacklog() {
        return socketBacklog;
    }

    @Override
    public boolean isSocketKeepalive() {
        return socketKeepalive;
    }

    @Override
    public int getHttpMaxContentLength() {
        return httpMaxContentLength;
    }
}

