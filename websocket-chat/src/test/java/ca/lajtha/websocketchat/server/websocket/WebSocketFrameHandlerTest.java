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

    private static final AttributeKey<String> SOCKET_ID_KEY = AttributeKey.valueOf("socketId");
    private static final AttributeKey<String> USER_ID_KEY = WebSocketHandshakeHandler.getUserIdKey();

    @Mock
    private MessageListener messageListener;
    
    private WebsocketManagerImpl websocketManager;
    private EmbeddedChannel channel;
    private WebSocketFrameHandler handler;

    @BeforeEach
    void setUp() {
        websocketManager = new WebsocketManagerImpl();
        websocketManager.addMessageListener(messageListener);
        handler = new WebSocketFrameHandler(websocketManager);
        
        // Create channel without handler first, set userId attribute, then add handler
        // This simulates the handshake handler setting the userId before channelActive is called
        channel = new EmbeddedChannel();
        channel.attr(USER_ID_KEY).set("test-user-id");
        channel.pipeline().addLast(handler);
        // Manually trigger channelActive since we added handler after channel creation
        channel.pipeline().fireChannelActive();
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
    void channelActive_generatesUniqueSocketIds() {
        // Arrange - reset mock to ignore the call from setUp, then use the class-level mock to capture socket IDs
        reset(messageListener);
        ArgumentCaptor<String> socketIdCaptor = ArgumentCaptor.forClass(String.class);
        
        WebsocketManagerImpl manager1 = new WebsocketManagerImpl();
        manager1.addMessageListener(messageListener);
        WebsocketManagerImpl manager2 = new WebsocketManagerImpl();
        manager2.addMessageListener(messageListener);
        
        WebSocketFrameHandler handler1 = new WebSocketFrameHandler(manager1);
        WebSocketFrameHandler handler2 = new WebSocketFrameHandler(manager2);
        
        // Create channels and set userId attributes before adding handlers
        EmbeddedChannel channel1 = new EmbeddedChannel();
        channel1.attr(USER_ID_KEY).set("test-user-1");
        channel1.pipeline().addLast(handler1);
        channel1.pipeline().fireChannelActive();
        
        EmbeddedChannel channel2 = new EmbeddedChannel();
        channel2.attr(USER_ID_KEY).set("test-user-2");
        channel2.pipeline().addLast(handler2);
        channel2.pipeline().fireChannelActive();

        // Act - channels become active, triggering playerConnected calls
        // The playerConnected method is called automatically when channel becomes active
        
        // Assert - capture socket IDs from ConnectionManager
        // Verify playerConnected was called twice and capture the socket IDs
        verify(messageListener, times(2)).playerConnected(socketIdCaptor.capture());
        
        // Get the captured socket IDs
        java.util.List<String> capturedSocketIds = socketIdCaptor.getAllValues();
        String socketId1 = capturedSocketIds.get(0);
        String socketId2 = capturedSocketIds.get(1);
        
        // Assert - socketIds should be unique
        assertNotNull(socketId1, "First socket ID should not be null");
        assertNotNull(socketId2, "Second socket ID should not be null");
        assertNotEquals(socketId1, socketId2, "Each connection should have a unique socketId");
        
        // Cleanup
        channel1.finish();
        channel2.finish();
    }

    @Test
    void channelRead0_withTextFrameAndSocketId_forwardsMessage() {
        // Arrange
        String testMessage = "Hello, server!";
        TextWebSocketFrame textFrame = new TextWebSocketFrame(testMessage);
        
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Get the socketId that was set during channelActive
        String socketId = channel.attr(SOCKET_ID_KEY).get();
        assertNotNull(socketId, "SocketId should be set during channelActive");
        
        // Reset mock to ignore the call from setUp
        reset(messageListener);
        
        // Act
        channel.writeInbound(textFrame);
        
        // Assert - verify that handlePlayerMessage was called with correct parameters
        ArgumentCaptor<String> socketIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageListener, times(1)).handlePlayerMessage(socketIdCaptor.capture(), messageCaptor.capture());
        
        assertEquals(socketId, socketIdCaptor.getValue(), "SocketId should match");
        assertEquals(testMessage, messageCaptor.getValue(), "Message should match");
    }

    @Test
    void channelRead0_withTextFrameAndNullSocketId_doesNotForwardMessage() {
        // Arrange
        String testMessage = "Hello, server!";
        TextWebSocketFrame textFrame = new TextWebSocketFrame(testMessage);
        
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Manually remove socketId
        channel.attr(SOCKET_ID_KEY).set(null);
        
        // Reset mock to ignore the call from setUp
        reset(messageListener);
        
        // Act
        channel.writeInbound(textFrame);
        
        // Assert - verify that handlePlayerMessage was NOT called
        verify(messageListener, never()).handlePlayerMessage(anyString(), anyString());
    }

    @Test
    void channelInactive_withSocketId_notifiesListener() {
        // Arrange
        // Read the welcome message to clear it
        channel.readOutbound();
        
        // Get the socketId that was set during channelActive
        String socketId = channel.attr(SOCKET_ID_KEY).get();
        assertNotNull(socketId, "SocketId should be set");
        
        // Reset mock to ignore the call from setUp
        reset(messageListener);
        
        // Act - close channel
        channel.close();
        
        // Assert - verify playerDisconnected was called with correct socketId
        ArgumentCaptor<String> socketIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageListener, times(1)).playerDisconnected(socketIdCaptor.capture());
        
        assertEquals(socketId, socketIdCaptor.getValue(), "SocketId should match");
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
        
        // Get the socketId
        String socketId = channel.attr(SOCKET_ID_KEY).get();
        
        // Reset mock to ignore the call from setUp
        reset(messageListener);
        
        // Act
        channel.writeInbound(frame1);
        channel.writeInbound(frame2);
        channel.writeInbound(frame3);
        
        // Assert - verify all messages were forwarded
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageListener, times(3)).handlePlayerMessage(eq(socketId), messageCaptor.capture());
        
        java.util.List<String> capturedMessages = messageCaptor.getAllValues();
        assertEquals(message1, capturedMessages.get(0));
        assertEquals(message2, capturedMessages.get(1));
        assertEquals(message3, capturedMessages.get(2));
    }
}

