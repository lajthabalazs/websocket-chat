package ca.lajtha;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketFrameHandlerTest {

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Channel channel;

    @Mock
    private SocketAddress remoteAddress;

    private WebSocketFrameHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketFrameHandler();
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(remoteAddress);
        when(channel.writeAndFlush(any())).thenReturn(null);
    }

    @Test
    void channelRead0_success() throws Exception {
        // Arrange
        String testMessage = "Hello, WebSocket!";
        TextWebSocketFrame frame = new TextWebSocketFrame(testMessage);

        // Act
        handler.channelRead0(ctx, frame);

        // Assert
        ArgumentCaptor<TextWebSocketFrame> captor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
        verify(channel, times(1)).writeAndFlush(captor.capture());
        
        TextWebSocketFrame response = captor.getValue();
        assertNotNull(response);
        assertEquals("Echo: " + testMessage, response.text());
    }

    @Test
    void channelRead0_failsIfUnsupportedFrameType() {
        // Arrange
        BinaryWebSocketFrame binaryFrame = new BinaryWebSocketFrame();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            handler.channelRead0(ctx, binaryFrame);
        });
    }

    @Test
    void channelRead0_withMultipleFrames() throws Exception {
        // Arrange
        TextWebSocketFrame frame1 = new TextWebSocketFrame("Message 1");
        TextWebSocketFrame frame2 = new TextWebSocketFrame("Message 2");

        // Act
        handler.channelRead0(ctx, frame1);
        handler.channelRead0(ctx, frame2);

        // Assert
        ArgumentCaptor<TextWebSocketFrame> captor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
        verify(channel, times(2)).writeAndFlush(captor.capture());
        
        assertEquals("Echo: Message 1", captor.getAllValues().get(0).text());
        assertEquals("Echo: Message 2", captor.getAllValues().get(1).text());
    }

    @Test
    void channelActive_success() {
        // Act
        handler.channelActive(ctx);

        // Assert
        ArgumentCaptor<TextWebSocketFrame> captor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
        verify(channel, times(1)).writeAndFlush(captor.capture());
        
        TextWebSocketFrame welcomeMessage = captor.getValue();
        assertNotNull(welcomeMessage);
        assertEquals("Welcome to the WebSocket server!", welcomeMessage.text());
        verify(ctx, times(1)).channel();
    }

    @Test
    void channelInactive_success() {
        // Act
        handler.channelInactive(ctx);

        // Assert - should not throw and should access remote address
        verify(ctx, atLeastOnce()).channel();
        verify(channel, atLeastOnce()).remoteAddress();
    }

    @Test
    void exceptionCaught_success() {
        // Arrange
        Throwable cause = new RuntimeException("Test exception");
        when(channel.close()).thenReturn(null);

        // Act
        handler.exceptionCaught(ctx, cause);

        // Assert
        verify(channel, times(1)).close();
    }
}

