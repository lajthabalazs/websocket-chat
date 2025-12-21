package ca.lajtha.websocketchat.server;

public interface ServerConfig {
    int getPort();
    int getHttpPort();
    String getWebsocketPath();
    int getSocketBacklog();
    boolean isSocketKeepalive();
    int getHttpMaxContentLength();
}
