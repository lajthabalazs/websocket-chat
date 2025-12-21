package ca.lajtha.websocketchat;

import com.google.inject.Inject;
import java.util.Properties;

public class PropertiesServerConfig implements ServerConfig {
    private final int port;
    private final int httpPort;
    private final String websocketPath;
    private final int socketBacklog;
    private final boolean socketKeepalive;
    private final int httpMaxContentLength;

    @Inject
    public PropertiesServerConfig(PropertiesLoader propertiesLoader) {
        Properties props = propertiesLoader.loadProperties();
        this.port = propertiesLoader.getIntProperty(props, "server.port", 8080);
        this.httpPort = propertiesLoader.getIntProperty(props, "http.server.port", 8081);
        this.websocketPath = propertiesLoader.getProperty(props, "websocket.path", "/websocket");
        this.socketBacklog = propertiesLoader.getIntProperty(props, "socket.backlog", 128);
        this.socketKeepalive = propertiesLoader.getBooleanProperty(props, "socket.keepalive", true);
        this.httpMaxContentLength = propertiesLoader.getIntProperty(props, "http.maxContentLength", 65536);
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public int getHttpPort() {
        return httpPort;
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

