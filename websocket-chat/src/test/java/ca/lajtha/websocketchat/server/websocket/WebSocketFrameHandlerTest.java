package ca.lajtha.websocketchat.server.websocket;

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
    private PlayerWebsocketConnectionManager websocketConnectionManager;
    
    private EmbeddedChannel channel;
    private WebSocketFrameHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketFrameHandler(websocketConnectionManager);
        channel = new EmbeddedChannel(handler);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finish();
        }
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
    void exceptionCaught_success() {
        // Arrange
        Throwable cause = new RuntimeException("Test exception");

        // Act
        channel.pipeline().fireExceptionCaught(cause);

        // Assert - channel should be closed after exception
        assertFalse(channel.isOpen());
    }


    @Test
    void channelActive_generatesUniquePlayerIds() {
        // Arrange - reset mock to ignore the call from setUp, then use the class-level mock to capture player IDs
        reset(websocketConnectionManager);
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<io.netty.channel.ChannelHandlerContext> contextCaptor = ArgumentCaptor.forClass(io.netty.channel.ChannelHandlerContext.class);
        
        WebSocketFrameHandler handler1 = new WebSocketFrameHandler(websocketConnectionManager);
        WebSocketFrameHandler handler2 = new WebSocketFrameHandler(websocketConnectionManager);
        EmbeddedChannel channel1 = new EmbeddedChannel(handler1);
        EmbeddedChannel channel2 = new EmbeddedChannel(handler2);

        // Act - channels become active, triggering playerConnected calls
        // The playerConnected method is called automatically when channel becomes active
        
        // Assert - capture player IDs from PlayerWebsocketConnectionManager
        // Verify playerConnected was called twice and capture the player IDs
        verify(websocketConnectionManager, times(2)).playerConnected(playerIdCaptor.capture(), contextCaptor.capture());
        
        // Get the captured player IDs
        java.util.List<String> capturedPlayerIds = playerIdCaptor.getAllValues();
        String playerId1 = capturedPlayerIds.get(0);
        String playerId2 = capturedPlayerIds.get(1);
        
        // Assert - playerIds should be unique
        assertNotNull(playerId1, "First player ID should not be null");
        assertNotNull(playerId2, "Second player ID should not be null");
        assertNotEquals(playerId1, playerId2, "Each connection should have a unique playerId");
        
        // Cleanup
        channel1.finish();
        channel2.finish();
    }

    @Test
    void channelRead0_withTextFrameAndPlayerId_forwardsMessage() {
        // Arrange
        String testMessage = "Hello, server!";
        TextWebSocketFrame textFrame = new TextWebSocketFrame(testMessage);
        
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Get the playerId that was set during channelActive
        String playerId = channel.attr(PLAYER_ID_KEY).get();
        assertNotNull(playerId, "PlayerId should be set during channelActive");
        
        // Reset mock to ignore the call from setUp
        reset(websocketConnectionManager);
        
        // Act
        channel.writeInbound(textFrame);
        
        // Assert - verify that handlePlayerMessage was called with correct parameters
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(websocketConnectionManager, times(1)).handlePlayerMessage(playerIdCaptor.capture(), messageCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getValue(), "PlayerId should match");
        assertEquals(testMessage, messageCaptor.getValue(), "Message should match");
    }

    @Test
    void channelRead0_withTextFrameAndNullPlayerId_doesNotForwardMessage() {
        // Arrange
        String testMessage = "Hello, server!";
        TextWebSocketFrame textFrame = new TextWebSocketFrame(testMessage);
        
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Manually remove playerId
        channel.attr(PLAYER_ID_KEY).set(null);
        
        // Reset mock to ignore the call from setUp
        reset(websocketConnectionManager);
        
        // Act
        channel.writeInbound(textFrame);
        
        // Assert - verify that handlePlayerMessage was NOT called
        verify(websocketConnectionManager, never()).handlePlayerMessage(anyString(), anyString());
    }

    @Test
    void channelActive_setsPlayerIdAndNotifiesListener() {
        // Arrange - create a new handler and channel to test channelActive
        reset(websocketConnectionManager);
        WebSocketFrameHandler newHandler = new WebSocketFrameHandler(websocketConnectionManager);
        EmbeddedChannel newChannel = new EmbeddedChannel(newHandler);
        
        try {
            // Act - channel becomes active automatically when created
            
            // Assert - verify playerId is set
            String playerId = newChannel.attr(PLAYER_ID_KEY).get();
            assertNotNull(playerId, "PlayerId should be set during channelActive");
            
            // Assert - verify playerConnected was called
            ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<io.netty.channel.ChannelHandlerContext> contextCaptor = ArgumentCaptor.forClass(io.netty.channel.ChannelHandlerContext.class);
            verify(websocketConnectionManager, times(1)).playerConnected(playerIdCaptor.capture(), contextCaptor.capture());
            
            assertEquals(playerId, playerIdCaptor.getValue(), "PlayerId should match");
            assertNotNull(contextCaptor.getValue(), "Context should not be null");
        } finally {
            newChannel.finish();
        }
    }

    @Test
    void channelInactive_withPlayerId_notifiesListener() {
        // Arrange
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Get the playerId that was set during channelActive
        String playerId = channel.attr(PLAYER_ID_KEY).get();
        assertNotNull(playerId, "PlayerId should be set");
        
        // Reset mock to ignore the call from setUp
        reset(websocketConnectionManager);
        
        // Act - close channel
        channel.close();
        
        // Assert - verify playerDisconnected was called with correct playerId
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(websocketConnectionManager, times(1)).playerDisconnected(playerIdCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getValue(), "PlayerId should match");
    }

    @Test
    void channelInactive_withoutPlayerId_notifiesListenerWithNull() {
        // Arrange - create a new handler and channel to test channelInactive
        reset(websocketConnectionManager);
        WebSocketFrameHandler newHandler = new WebSocketFrameHandler(websocketConnectionManager);
        EmbeddedChannel testChannel = new EmbeddedChannel(newHandler);
        
        try {
            // Read welcome message
            testChannel.readOutbound();
            
            // Manually remove playerId (it was set during channelActive)
            testChannel.attr(PLAYER_ID_KEY).set(null);
            
            // Reset mock to ignore the channelActive call
            reset(websocketConnectionManager);

            // Act - close channel
            testChannel.close();
            
            // Assert - verify playerDisconnected was called with null (it still gets called, just with null)
            ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(websocketConnectionManager, times(1)).playerDisconnected(playerIdCaptor.capture());
            
            assertNull(playerIdCaptor.getValue(), "PlayerId should be null");
        } finally {
            testChannel.finish();
        }
    }

    @Test
    void channelRead0_withMultipleTextFrames_forwardsAllMessages() {
        // Arrange
        String message1 = "First message";
        String message2 = "Second message";
        String message3 = "Third message";
        
        TextWebSocketFrame frame1 = new TextWebSocketFrame(message1);
        TextWebSocketFrame frame2 = new TextWebSocketFrame(message2);
        TextWebSocketFrame frame3 = new TextWebSocketFrame(message3);
        
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Get the playerId
        String playerId = channel.attr(PLAYER_ID_KEY).get();
        
        // Reset mock to ignore the call from setUp
        reset(websocketConnectionManager);
        
        // Act
        channel.writeInbound(frame1);
        channel.writeInbound(frame2);
        channel.writeInbound(frame3);
        
        // Assert - verify all messages were forwarded
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(websocketConnectionManager, times(3)).handlePlayerMessage(eq(playerId), messageCaptor.capture());
        
        java.util.List<String> capturedMessages = messageCaptor.getAllValues();
        assertEquals(message1, capturedMessages.get(0));
        assertEquals(message2, capturedMessages.get(1));
        assertEquals(message3, capturedMessages.get(2));
    }
}

