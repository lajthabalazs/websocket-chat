package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.server.websocket.PlayerWebsocketConnectionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerConnectionManagerTest {

    private PlayerWebsocketConnectionManager connectionManager;
    private EmbeddedChannel channel1;
    private EmbeddedChannel channel2;

    @BeforeEach
    void setUp() {
        connectionManager = new PlayerWebsocketConnectionManager();
        // Create channels with a handler to get a valid context
        channel1 = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        channel2 = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    }

    @AfterEach
    void tearDown() {
        if (channel1 != null) {
            channel1.finish();
        }
        if (channel2 != null) {
            channel2.finish();
        }
    }

    private ChannelHandlerContext getContext(EmbeddedChannel channel) {
        // Get the context from the first handler in the pipeline
        var handler = channel.pipeline().first();
        return channel.pipeline().context(handler);
    }

    @Test
    void registerPlayer_addsPlayer() {
        // Arrange
        String playerId = "player1";

        // Act
        connectionManager.registerPlayer(playerId, getContext(channel1));

        // Assert
        assertTrue(connectionManager.isPlayerConnected(playerId));
        assertTrue(connectionManager.getConnectedPlayers().contains(playerId));
    }

    @Test
    void unregisterPlayer_removesPlayer() {
        // Arrange
        String playerId = "player1";
        connectionManager.registerPlayer(playerId, getContext(channel1));

        // Act
        connectionManager.unregisterPlayer(playerId);

        // Assert
        assertFalse(connectionManager.isPlayerConnected(playerId));
        assertFalse(connectionManager.getConnectedPlayers().contains(playerId));
    }

    @Test
    void sendToPlayer_sendsMessage() {
        // Arrange
        String playerId = "player1";
        String message = "Test message";
        connectionManager.registerPlayer(playerId, getContext(channel1));

        // Act
        boolean sent = connectionManager.sendToPlayer(playerId, message);

        // Assert
        assertTrue(sent);
        TextWebSocketFrame frame = channel1.readOutbound();
        assertNotNull(frame);
        assertEquals(message, frame.text());
        frame.release();
    }

    @Test
    void sendToPlayer_returnsFalseForNonExistentPlayer() {
        // Arrange
        String playerId = "nonexistent";
        String message = "Test message";

        // Act
        boolean sent = connectionManager.sendToPlayer(playerId, message);

        // Assert
        assertFalse(sent);
    }

    @Test
    void getConnectedPlayers_returnsAllPlayers() {
        // Arrange
        String playerId1 = "player1";
        String playerId2 = "player2";
        connectionManager.registerPlayer(playerId1, getContext(channel1));
        connectionManager.registerPlayer(playerId2, getContext(channel2));

        // Act
        var players = connectionManager.getConnectedPlayers();

        // Assert
        assertEquals(2, players.size());
        assertTrue(players.contains(playerId1));
        assertTrue(players.contains(playerId2));
    }

    @Test
    void getConnectedPlayers_removesInactiveChannels() {
        // Arrange
        String playerId1 = "player1";
        String playerId2 = "player2";
        connectionManager.registerPlayer(playerId1, getContext(channel1));
        connectionManager.registerPlayer(playerId2, getContext(channel2));

        // Act - close one channel
        channel1.close();
        var players = connectionManager.getConnectedPlayers();

        // Assert - inactive channel should be removed
        assertEquals(1, players.size());
        assertFalse(players.contains(playerId1));
        assertTrue(players.contains(playerId2));
    }

    @Test
    void isPlayerConnected_returnsFalseForNonExistentPlayer() {
        // Act
        boolean connected = connectionManager.isPlayerConnected("nonexistent");

        // Assert
        assertFalse(connected);
    }
}

