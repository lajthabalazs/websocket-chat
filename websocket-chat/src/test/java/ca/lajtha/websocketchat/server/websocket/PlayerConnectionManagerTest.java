package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.auth.TokenManager;
import ca.lajtha.websocketchat.game.Game;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
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
class PlayerConnectionManagerTest {

    private PlayerWebsocketConnectionManager connectionManager;
    @Mock private Game game;
    @Mock private TokenManager tokenManager;
    @Mock private WebsocketManager websocketManager;
    private EmbeddedChannel channel1;
    private EmbeddedChannel channel2;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        connectionManager = new PlayerWebsocketConnectionManager(game, tokenManager, websocketManager);
        objectMapper = new ObjectMapper();
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
    void sendToPlayer_sendsMessage() {
        // Arrange
        String socketId = "socket1";
        String message = "Test message";
        connectionManager.playerConnected(socketId);

        // Act
        boolean sent = connectionManager.sendToPlayer(socketId, message);

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
        String socketId = "nonexistent";
        String message = "Test message";

        // Act
        boolean sent = connectionManager.sendToPlayer(socketId, message);

        // Assert
        assertFalse(sent);
    }

    @Test
    void handlePlayerMessage_withValidTokenVerification_authenticatesPlayer() throws Exception {
        // Arrange
        String socketId = "socket1";
        String token = "valid-jwt-token";
        String userId = "user123";
        String tokenVerificationJson = objectMapper.writeValueAsString(
            new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token));
        
        connectionManager.playerConnected(socketId);
        when(tokenManager.getUserIdFromToken(token)).thenReturn(userId);

        // Act
        connectionManager.handlePlayerMessage(socketId, tokenVerificationJson);

        // Assert
        assertTrue(connectionManager.isAuthenticated(socketId));
        assertEquals(userId, connectionManager.getUserId(socketId));
        assertEquals(socketId, connectionManager.getSocketId(userId));
        verify(game).onPlayerConnected(userId);
        
        // Verify response was sent
        TextWebSocketFrame responseFrame = channel1.readOutbound();
        assertNotNull(responseFrame);
        String responseJson = responseFrame.text();
        assertTrue(responseJson.contains("\"success\":true"));
        responseFrame.release();
    }

    @Test
    void handlePlayerMessage_withInvalidToken_doesNotAuthenticatePlayer() throws Exception {
        // Arrange
        String socketId = "socket1";
        String token = "invalid-token";
        String tokenVerificationJson = objectMapper.writeValueAsString(
            new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token));
        
        connectionManager.playerConnected(socketId);
        when(tokenManager.getUserIdFromToken(token)).thenReturn(null);

        // Act
        connectionManager.handlePlayerMessage(socketId, tokenVerificationJson);

        // Assert
        assertFalse(connectionManager.isAuthenticated(socketId));
        assertNull(connectionManager.getUserId(socketId));
        verify(game, never()).onPlayerConnected(anyString());
        
        // Verify failure response was sent
        TextWebSocketFrame responseFrame = channel1.readOutbound();
        assertNotNull(responseFrame);
        String responseJson = responseFrame.text();
        assertTrue(responseJson.contains("\"success\":false"));
        responseFrame.release();
    }

    @Test
    void handlePlayerMessage_withEmptyToken_doesNotAuthenticatePlayer() throws Exception {
        // Arrange
        String socketId = "socket1";
        String tokenVerificationJson = objectMapper.writeValueAsString(
            new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, ""));
        
        connectionManager.playerConnected(socketId);

        // Act
        connectionManager.handlePlayerMessage(socketId, tokenVerificationJson);

        // Assert
        assertFalse(connectionManager.isAuthenticated(socketId));
        verify(tokenManager, never()).getUserIdFromToken(anyString());
        verify(game, never()).onPlayerConnected(anyString());
        
        // Verify failure response was sent
        TextWebSocketFrame responseFrame = channel1.readOutbound();
        assertNotNull(responseFrame);
        String responseJson = responseFrame.text();
        assertTrue(responseJson.contains("\"success\":false"));
        responseFrame.release();
    }

    @Test
    void handlePlayerMessage_withoutAuthentication_rejectsGameMessage() throws Exception {
        // Arrange
        String socketId = "socket1";
        String gameMessage = "{\"type\":\"getMessages\"}";
        
        connectionManager.playerConnected(socketId);

        // Act
        connectionManager.handlePlayerMessage(socketId, gameMessage);

        // Assert
        verify(game, never()).handlePlayerMessage(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withAuthentication_forwardsToGameWithUserId() throws Exception {
        // Arrange
        String socketId = "socket1";
        String userId = "user123";
        String token = "valid-token";
        String gameMessage = "{\"type\":\"getMessages\"}";
        
        connectionManager.playerConnected(socketId);
        when(tokenManager.getUserIdFromToken(token)).thenReturn(userId);
        
        // Authenticate first
        String tokenVerificationJson = objectMapper.writeValueAsString(
            new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token));
        connectionManager.handlePlayerMessage(socketId, tokenVerificationJson);
        ((TextWebSocketFrame) channel1.readOutbound()).release(); // Clear the auth response

        // Act
        connectionManager.handlePlayerMessage(socketId, gameMessage);

        // Assert
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(game).handlePlayerMessage(userIdCaptor.capture(), messageCaptor.capture());
        assertEquals(userId, userIdCaptor.getValue());
        assertEquals(gameMessage, messageCaptor.getValue());
    }

    @Test
    void sendToPlayer_withUserId_translatesToSocketId() {
        // Arrange
        String socketId = "socket1";
        String userId = "user123";
        String token = "valid-token";
        String message = "Test message";
        
        connectionManager.playerConnected(socketId);
        when(tokenManager.getUserIdFromToken(token)).thenReturn(userId);
        
        // Authenticate
        try {
            String tokenVerificationJson = objectMapper.writeValueAsString(
                new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token));
            connectionManager.handlePlayerMessage(socketId, tokenVerificationJson);
            ((TextWebSocketFrame) channel1.readOutbound()).release(); // Clear the auth response
        } catch (Exception e) {
            fail("Failed to authenticate socket", e);
        }

        // Act
        boolean sent = connectionManager.sendToPlayer(userId, message);

        // Assert
        assertTrue(sent);
        TextWebSocketFrame frame = channel1.readOutbound();
        assertNotNull(frame);
        assertEquals(message, frame.text());
        frame.release();
    }

    @Test
    void playerDisconnected_whenAuthenticated_notifiesGameWithUserId() {
        // Arrange
        String socketId = "socket1";
        String userId = "user123";
        String token = "valid-token";
        
        connectionManager.playerConnected(socketId);
        when(tokenManager.getUserIdFromToken(token)).thenReturn(userId);
        
        // Authenticate
        try {
            String tokenVerificationJson = objectMapper.writeValueAsString(
                new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token));
            connectionManager.handlePlayerMessage(socketId, tokenVerificationJson);
            ((TextWebSocketFrame) channel1.readOutbound()).release(); // Clear the auth response
        } catch (Exception e) {
            fail("Failed to authenticate socket", e);
        }

        // Act
        connectionManager.playerDisconnected(socketId);

        // Assert
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(game).onPlayerDisconnected(userIdCaptor.capture());
        assertEquals(userId, userIdCaptor.getValue());
        assertFalse(connectionManager.isAuthenticated(socketId));
        assertNull(connectionManager.getUserId(socketId));
    }

    @Test
    void playerDisconnected_whenNotAuthenticated_doesNotNotifyGame() {
        // Arrange
        String socketId = "socket1";
        connectionManager.playerConnected(socketId);

        // Act
        connectionManager.playerDisconnected(socketId);

        // Assert
        verify(game, never()).onPlayerDisconnected(anyString());
    }

    @Test
    void handlePlayerMessage_withReauthentication_updatesUserIdMapping() throws Exception {
        // Arrange
        String socketId = "socket1";
        String userId1 = "user123";
        String userId2 = "user456";
        String token1 = "token1";
        String token2 = "token2";
        
        connectionManager.playerConnected(socketId);
        when(tokenManager.getUserIdFromToken(token1)).thenReturn(userId1);
        when(tokenManager.getUserIdFromToken(token2)).thenReturn(userId2);

        // Authenticate with first token
        String tokenVerificationJson1 = objectMapper.writeValueAsString(
            new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token1));
        connectionManager.handlePlayerMessage(socketId, tokenVerificationJson1);
        ((TextWebSocketFrame) channel1.readOutbound()).release();
        
        // Act - Re-authenticate with different token
        String tokenVerificationJson2 = objectMapper.writeValueAsString(
            new TokenVerificationRequest(TokenVerificationRequest.MESSAGE_TYPE, token2));
        connectionManager.handlePlayerMessage(socketId, tokenVerificationJson2);

        // Assert
        assertEquals(userId2, connectionManager.getUserId(socketId));
        assertEquals(socketId, connectionManager.getSocketId(userId2));
        assertNull(connectionManager.getSocketId(userId1)); // Old mapping should be removed
        verify(game).onPlayerDisconnected(userId1); // Old user should be disconnected
        verify(game).onPlayerConnected(userId2); // New user should be connected
    }

    @Test
    void getUserId_returnsNullForUnauthenticatedSocket() {
        // Arrange
        String socketId = "socket1";
        connectionManager.playerConnected(socketId);

        // Act
        String userId = connectionManager.getUserId(socketId);

        // Assert
        assertNull(userId);
    }

    @Test
    void getSocketId_returnsNullForUnknownUserId() {
        // Act
        String socketId = connectionManager.getSocketId("unknown-user");

        // Assert
        assertNull(socketId);
    }

    @Test
    void isAuthenticated_returnsFalseForUnauthenticatedSocket() {
        // Arrange
        String socketId = "socket1";
        connectionManager.playerConnected(socketId);

        // Act
        boolean authenticated = connectionManager.isAuthenticated(socketId);

        // Assert
        assertFalse(authenticated);
    }
}

