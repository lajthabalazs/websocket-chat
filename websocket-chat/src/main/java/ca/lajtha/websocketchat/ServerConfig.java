package ca.lajtha.websocketchat;

public interface ServerConfig {
    int getPort();
    int getHttpPort();
    String getWebsocketPath();
    int getSocketBacklog();
    boolean isSocketKeepalive();
    int getHttpMaxContentLength();
}
