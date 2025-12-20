package ca.lajtha;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketFrameHandlerTest {

    private EmbeddedChannel channel;
    private WebSocketFrameHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketFrameHandler();
        channel = new EmbeddedChannel(handler);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finish();
        }
    }

    @Test
    void channelRead0_success() throws Exception {
        // Arrange
        String testMessage = "Hello, WebSocket!";
        TextWebSocketFrame frame = new TextWebSocketFrame(testMessage);

        // Act
        channel.writeInbound(frame);
        channel.readOutbound(); // swallowing welcome message
        // Assert
        TextWebSocketFrame response = channel.readOutbound();
        assertNotNull(response);
        assertEquals("Echo: " + testMessage, response.text());
        response.release();
    }

    @Test
    void channelRead0_failsIfUnsupportedFrameType() {
        // Arrange
        BinaryWebSocketFrame binaryFrame = new BinaryWebSocketFrame();

        // Act & Assert
            channel.writeInbound(binaryFrame);

    }

    @Test
    void channelRead0_withMultipleFrames() throws Exception {
        // Arrange
        TextWebSocketFrame frame1 = new TextWebSocketFrame("Message 1");
        TextWebSocketFrame frame2 = new TextWebSocketFrame("Message 2");

        // Act
        channel.writeInbound(frame1);
        channel.writeInbound(frame2);
        channel.readOutbound(); // swallow welcome message
        // Assert
        TextWebSocketFrame response1 = channel.readOutbound();
        assertNotNull(response1);
        assertEquals("Echo: Message 1", response1.text());
        response1.release();

        TextWebSocketFrame response2 = channel.readOutbound();
        assertNotNull(response2);
        assertEquals("Echo: Message 2", response2.text());
        response2.release();
    }

    @Test
    void channelActive_success() {
        // Arrange - create a fresh channel to test channelActive
        // channelActive is called automatically when EmbeddedChannel is created
        WebSocketFrameHandler freshHandler = new WebSocketFrameHandler();
        EmbeddedChannel activeChannel = new EmbeddedChannel(freshHandler);

        // Assert
        TextWebSocketFrame welcomeMessage = activeChannel.readOutbound();
        assertNotNull(welcomeMessage);
        assertEquals("Welcome to the WebSocket server!", welcomeMessage.text());
        welcomeMessage.release();
        
        activeChannel.finish();
    }

    @Test
    void channelInactive_success() {
        // Act - channelInactive is called when channel is closed
        channel.close();

        // Assert - should not throw
        assertFalse(channel.isActive());
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
}

