package ca.lajtha.websocketchat;

public interface ServerConfig {
    int getPort();
    String getWebsocketPath();
    int getSocketBacklog();
    boolean isSocketKeepalive();
    int getHttpMaxContentLength();
}
