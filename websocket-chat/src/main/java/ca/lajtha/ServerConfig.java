package ca.lajtha;

public interface ServerConfig {
    int getPort();
    String getWebsocketPath();
    int getSocketBacklog();
    boolean isSocketKeepalive();
    int getHttpMaxContentLength();
}
