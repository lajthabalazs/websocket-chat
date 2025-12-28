package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.game.Game;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketFrameHandlerTest {

    private static final AttributeKey<String> SOCKET_ID_KEY = AttributeKey.valueOf("socketId");
    private static final AttributeKey<String> USER_ID_KEY = WebSocketHandshakeHandler.getUserIdKey();

    @Mock
    private Game game;
    
    private WebsocketManagerImpl websocketManager;
    private EmbeddedChannel channel;
    private WebSocketFrameHandler handler;

    @BeforeEach
    void setUp() {
        websocketManager = new WebsocketManagerImpl();
        websocketManager.setGame(game);
        handler = new WebSocketFrameHandler(websocketManager);
        
        // Create channel without handler first, set userId attribute, then add handler
        // This simulates the handshake handler setting the userId before channelActive is called
        channel = new EmbeddedChannel();
        channel.attr(USER_ID_KEY).set("test-user-id");
        channel.pipeline().addLast(handler);
        // Manually trigger channelActive since we added handler after channel creation
        channel.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete(null, null, null));

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
        // Arrange - create separate game mocks for each manager
        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);
        
        WebsocketManagerImpl manager1 = new WebsocketManagerImpl();
        manager1.setGame(game1);
        WebsocketManagerImpl manager2 = new WebsocketManagerImpl();
        manager2.setGame(game2);
        
        WebSocketFrameHandler handler1 = new WebSocketFrameHandler(manager1);
        WebSocketFrameHandler handler2 = new WebSocketFrameHandler(manager2);
        
        // Create channels and set userId attributes before adding handlers
        EmbeddedChannel channel1 = new EmbeddedChannel();
        channel1.attr(USER_ID_KEY).set("test-user-1");
        channel1.pipeline().addLast(handler1);
        channel1.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete(null, null, null));
        
        EmbeddedChannel channel2 = new EmbeddedChannel();
        channel2.attr(USER_ID_KEY).set("test-user-2");
        channel2.pipeline().addLast(handler2);
        channel2.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete(null, null, null));
        
        // Assert - verify that handlePlayerConnected was called for each user
        verify(game1, times(1)).handlePlayerConnected("test-user-1");
        verify(game2, times(1)).handlePlayerConnected("test-user-2");
        
        // Assert - verify socket IDs are unique
        String socketId1 = channel1.attr(SOCKET_ID_KEY).get();
        String socketId2 = channel2.attr(SOCKET_ID_KEY).get();
        assertNotNull(socketId1, "SocketId1 should be set");
        assertNotNull(socketId2, "SocketId2 should be set");
        assertNotEquals(socketId1, socketId2, "Socket IDs should be unique");
        
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
        reset(game);

        
        // Act
        channel.writeInbound(textFrame);
        
        // Assert - verify that handlePlayerMessage was called with correct parameters
        verify(game, times(1)).handlePlayerMessage("test-user-id", testMessage);
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
        reset(game);

        
        // Act
        channel.writeInbound(textFrame);
        
        // Assert - verify that handlePlayerMessage was NOT called when socketId is null
        verify(game, never()).handlePlayerMessage(anyString(), anyString());
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
        reset(game);

        
        // Act - close channel
        channel.close();
        
        // Assert - verify that handlePlayerDisconnected was called with correct userId
        verify(game, times(1)).handlePlayerDisconnected("test-user-id");
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
        
        // Reset mock to ignore the call from setUp
        reset(game);

        
        // Act
        channel.writeInbound(frame1);
        channel.writeInbound(frame2);
        channel.writeInbound(frame3);
        
        // Assert - verify that handlePlayerMessage was called for each message with correct parameters
        verify(game, times(1)).handlePlayerMessage("test-user-id", message1);
        verify(game, times(1)).handlePlayerMessage("test-user-id", message2);
        verify(game, times(1)).handlePlayerMessage("test-user-id", message3);
        verify(game, times(3)).handlePlayerMessage(eq("test-user-id"), anyString());
    }
}

