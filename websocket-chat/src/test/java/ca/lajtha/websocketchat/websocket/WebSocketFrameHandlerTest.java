package ca.lajtha.websocketchat.websocket;

import ca.lajtha.websocketchat.game.Game;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketFrameHandlerTest {

    private static final AttributeKey<String> PLAYER_ID_KEY = AttributeKey.valueOf("playerId");
    
    @Mock
    private Game game;

    
    private EmbeddedChannel channel;
    private WebSocketFrameHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketFrameHandler(game);
        channel = new EmbeddedChannel(handler);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finish();
        }
    }

    @Test
    void channelRead0_forwardsMessageToGame() throws Exception {
        // Arrange
        String testMessage = "Hello, WebSocket!";
        TextWebSocketFrame frame = new TextWebSocketFrame(testMessage);
        
        // Read the welcome message to clear it and get the playerId
        TextWebSocketFrame welcomeMessage = channel.readOutbound();
        assertNotNull(welcomeMessage);
        String playerId = extractPlayerIdFromWelcome(welcomeMessage.text());
        assertNotNull(playerId);
        welcomeMessage.release();

        // Act
        channel.writeInbound(frame);

        // Assert - message should be forwarded to game
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(game, times(1)).handlePlayerMessage(playerIdCaptor.capture(), messageCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getValue());
        assertEquals(testMessage, messageCaptor.getValue());
    }

    @Test
    void channelRead0_failsIfUnsupportedFrameType() {
        // Arrange
        BinaryWebSocketFrame binaryFrame = new BinaryWebSocketFrame();
        
        // Read the welcome message to clear it
        channel.readOutbound();

        // Act - write unsupported frame type
        // The exception will be caught by Netty and handled via exceptionCaught
        channel.writeInbound(binaryFrame);

        // Assert - channel should be closed due to exception handling
        // EmbeddedChannel processes exceptions internally, so we verify the channel is closed
        assertFalse(channel.isOpen(), "Channel should be closed after unsupported frame type exception");
    }

    @Test
    void channelRead0_withMultipleFrames() throws Exception {
        // Arrange
        TextWebSocketFrame frame1 = new TextWebSocketFrame("Message 1");
        TextWebSocketFrame frame2 = new TextWebSocketFrame("Message 2");
        
        // Read the welcome message to clear it and get the playerId
        TextWebSocketFrame welcomeMessage = channel.readOutbound();
        assertNotNull(welcomeMessage);
        String playerId = extractPlayerIdFromWelcome(welcomeMessage.text());
        welcomeMessage.release();

        // Act
        channel.writeInbound(frame1);
        channel.writeInbound(frame2);

        // Assert - both messages should be forwarded to game
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(game, times(2)).handlePlayerMessage(playerIdCaptor.capture(), messageCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getAllValues().get(0));
        assertEquals(playerId, playerIdCaptor.getAllValues().get(1));
        assertEquals("Message 1", messageCaptor.getAllValues().get(0));
        assertEquals("Message 2", messageCaptor.getAllValues().get(1));
    }

    @Test
    void channelActive_generatesPlayerIdAndNotifiesGame() {
        // Arrange - create a fresh channel to test channelActive
        WebSocketFrameHandler freshHandler = new WebSocketFrameHandler(game);
        EmbeddedChannel activeChannel = new EmbeddedChannel(freshHandler);

        // Assert - welcome message should contain playerId
        TextWebSocketFrame welcomeMessage = activeChannel.readOutbound();
        assertNotNull(welcomeMessage);
        assertTrue(welcomeMessage.text().contains("Welcome to the WebSocket server!"));
        assertTrue(welcomeMessage.text().contains("Your player ID:"));
        
        String playerId = extractPlayerIdFromWelcome(welcomeMessage.text());
        assertNotNull(playerId);
        assertFalse(playerId.isEmpty());
        
        // Verify playerId is stored in channel attributes
        String storedPlayerId = activeChannel.attr(PLAYER_ID_KEY).get();
        assertEquals(playerId, storedPlayerId);

        // Verify game was notified
        verify(game, times(1)).onPlayerConnected(playerId);
        
        welcomeMessage.release();
        activeChannel.finish();
    }

    @Test
    void channelInactive_notifiesGame() {
        // Arrange - get the playerId from the welcome message
        TextWebSocketFrame welcomeMessage = channel.readOutbound();
        assertNotNull(welcomeMessage);
        String playerId = extractPlayerIdFromWelcome(welcomeMessage.text());
        welcomeMessage.release();
        
        // Act - channelInactive is called when channel is closed
        channel.close();

        // Assert - should not throw and game should be notified
        assertFalse(channel.isActive());
        verify(game, times(1)).onPlayerDisconnected(playerId);
    }

    @Test
    void exceptionCaught_success() {
        // Arrange
        Throwable cause = new RuntimeException("Test exception");

        // Act
        channel.pipeline().fireExceptionCaught(cause);

        // Assert - channel should be closed after exception
        assertFalse(channel.isOpen());
    }

    @Test
    void channelRead0_withoutPlayerId_doesNotForwardToGame() {
        // Arrange - create a handler and channel, but manually remove playerId attribute
        // to simulate edge case where message arrives before playerId is set
        WebSocketFrameHandler testHandler = new WebSocketFrameHandler(game);
        EmbeddedChannel testChannel = new EmbeddedChannel(testHandler);
        
        // Manually remove playerId to simulate edge case
        testChannel.attr(PLAYER_ID_KEY).set(null);
        
        String testMessage = "Test message";
        TextWebSocketFrame frame = new TextWebSocketFrame(testMessage);

        // Act
        testChannel.writeInbound(frame);

        // Assert - game should not be called since playerId is null
        verify(game, never()).handlePlayerMessage(anyString(), anyString());
        
        testChannel.finish();
    }

    @Test
    void channelRead0_withEmptyMessage() {
        // Arrange
        String emptyMessage = "";
        TextWebSocketFrame frame = new TextWebSocketFrame(emptyMessage);
        
        // Read the welcome message to clear it and get the playerId
        TextWebSocketFrame welcomeMessage = channel.readOutbound();
        assertNotNull(welcomeMessage);
        String playerId = extractPlayerIdFromWelcome(welcomeMessage.text());
        welcomeMessage.release();

        // Act
        channel.writeInbound(frame);

        // Assert - empty message should still be forwarded to game
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(game, times(1)).handlePlayerMessage(playerIdCaptor.capture(), messageCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getValue());
        assertEquals(emptyMessage, messageCaptor.getValue());
    }

    @Test
    void channelActive_generatesUniquePlayerIds() {
        // Arrange - create two separate channels
        WebSocketFrameHandler handler1 = new WebSocketFrameHandler(game);
        WebSocketFrameHandler handler2 = new WebSocketFrameHandler(game);
        EmbeddedChannel channel1 = new EmbeddedChannel(handler1);
        EmbeddedChannel channel2 = new EmbeddedChannel(handler2);

        // Act - get playerIds from welcome messages
        TextWebSocketFrame welcome1 = channel1.readOutbound();
        TextWebSocketFrame welcome2 = channel2.readOutbound();
        
        String playerId1 = extractPlayerIdFromWelcome(welcome1.text());
        String playerId2 = extractPlayerIdFromWelcome(welcome2.text());
        
        welcome1.release();
        welcome2.release();

        // Assert - playerIds should be unique
        assertNotNull(playerId1);
        assertNotNull(playerId2);
        assertNotEquals(playerId1, playerId2, "Each connection should have a unique playerId");
        
        channel1.finish();
        channel2.finish();
    }

    @Test
    void channelInactive_withoutPlayerId_doesNotNotifyGame() {
        // Arrange - create a channel but remove playerId before closing
        WebSocketFrameHandler testHandler = new WebSocketFrameHandler(game);
        EmbeddedChannel testChannel = new EmbeddedChannel(testHandler);
        
        // Read welcome message
        testChannel.readOutbound();
        
        // Manually remove playerId
        testChannel.attr(PLAYER_ID_KEY).set(null);

        // Act - close channel
        testChannel.close();

        // Assert - game should not be notified since playerId is null
        verify(game, never()).onPlayerDisconnected(anyString());
        
        testChannel.finish();
    }
    
    /**
     * Extracts the playerId from the welcome message.
     * Expected format: "Welcome to the WebSocket server! Your player ID: <playerId>"
     */
    private String extractPlayerIdFromWelcome(String welcomeText) {
        String prefix = "Your player ID: ";
        int index = welcomeText.indexOf(prefix);
        if (index == -1) {
            return null;
        }
        return welcomeText.substring(index + prefix.length()).trim();
    }
}

