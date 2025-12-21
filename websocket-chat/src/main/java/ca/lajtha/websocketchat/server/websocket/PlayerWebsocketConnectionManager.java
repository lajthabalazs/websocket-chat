package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.auth.TokenManager;
import ca.lajtha.websocketchat.game.Game;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player connections and allows sending messages to players.
 * Handles JWT token verification and translates between socketId and userId.
 */
public class PlayerWebsocketConnectionManager implements PlayerConnectionListener, PlayerMessageSender {
    private final Map<String, String> socketIdToUserId = new ConcurrentHashMap<>();
    private final Map<String, String> userIdToSocketId = new ConcurrentHashMap<>();
    private final Set<String> authenticatedSockets = ConcurrentHashMap.newKeySet();
    private final Game game;
    private final TokenManager tokenManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebsocketManager websocketManager;

    @Inject
    PlayerWebsocketConnectionManager(Game game, TokenManager tokenManager, WebsocketManager websocketManager) {
        this.game = game;
        this.tokenManager = tokenManager;
        this.websocketManager = websocketManager;
    }

    @Override
    public void playerConnected(String socketId) {
        // TODO do nothing for now
    }

    @Override
    public void playerDisconnected(String socketId) {
        if(!socketIdToUserId.containsKey(socketId)){
            var userId = socketIdToUserId.remove(socketId);
            game.onPlayerDisconnected(userId);
        }
    }

    @Override
    public void handlePlayerMessage(String socketId, String request) {
        // Check if this is a token verification message
        if (isTokenVerificationMessage(request)) {
            handleTokenVerification(socketId, request);
            return;
        }
        
        // Only allow authenticated sockets to communicate with the game
        if (!authenticatedSockets.contains(socketId)) {
            System.err.println("Rejected message from unauthenticated socket: " + socketId);
            return;
        }
        
        // Translate socketId to userId when communicating with the game
        String userId = socketIdToUserId.get(socketId);
        if (userId == null) {
            System.err.println("Socket " + socketId + " is authenticated but has no userId mapping");
            return;
        }
        
        game.handlePlayerMessage(userId, request);
    }
    
    /**
     * Checks if the message is a token verification request.
     */
    private boolean isTokenVerificationMessage(String message) {
        try {
            TokenVerificationRequest request = objectMapper.readValue(message, TokenVerificationRequest.class);
            return TokenVerificationRequest.MESSAGE_TYPE.equals(request.type());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Handles token verification message from a client.
     */
    private void handleTokenVerification(String socketId, String request) {
        try {
            TokenVerificationRequest verificationRequest = objectMapper.readValue(request, TokenVerificationRequest.class);
            String token = verificationRequest.token();
            
            if (token == null || token.isEmpty()) {
                sendTokenVerificationResponse(socketId, TokenVerificationResponse.failure("Token is required"));
                return;
            }
            
            // Use token manager to get userId from JWT
            String userId = tokenManager.getUserIdFromToken(token);
            
            if (userId == null) {
                sendTokenVerificationResponse(socketId, TokenVerificationResponse.failure("Invalid or expired token"));
                return;
            }
            
            // If socket was already authenticated with a different userId, remove old mapping
            String oldUserId = socketIdToUserId.get(socketId);
            if (oldUserId != null && !oldUserId.equals(userId)) {
                userIdToSocketId.remove(oldUserId);
                game.onPlayerDisconnected(oldUserId);
            }
            
            // Store the mapping between socketId and userId
            socketIdToUserId.put(socketId, userId);
            userIdToSocketId.put(userId, socketId);
            authenticatedSockets.add(socketId);
            
            // Notify game that player connected (using userId)
            game.onPlayerConnected(userId);
            
            // Send success response
            sendTokenVerificationResponse(socketId, TokenVerificationResponse.success());
            
        } catch (Exception e) {
            System.err.println("Error handling token verification for socket " + socketId + ": " + e.getMessage());
            e.printStackTrace();
            sendTokenVerificationResponse(socketId, TokenVerificationResponse.failure("Error processing token verification"));
        }
    }
    
    /**
     * Sends a token verification response to the client.
     */
    private void sendTokenVerificationResponse(String socketId, TokenVerificationResponse response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            sendToPlayer(socketId, responseJson);
        } catch (Exception e) {
            System.err.println("Error sending token verification response to socket " + socketId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to a specific player.
     * Accepts either a socketId or userId - if userId is provided, it will be translated to socketId.
     * 
     * @param playerId the unique identifier of the socket or user (can be socketId or userId)
     * @param message the message to send
     * @return true if the message was sent, false if the player is not connected
     */
    @Override
    public boolean sendToPlayer(String playerId, String message) {
        String socketId = userIdToSocketId.getOrDefault(playerId, null);
        
        // Check if socket is connected before sending
        if (socketId != null) {
            websocketManager.sendMessage(socketId, message);
            return true;
        }
        return false;
    }
    
    /**
     * Gets the userId for a given socketId.
     * 
     * @param socketId the socket identifier
     * @return the userId if the socket is authenticated, null otherwise
     */
    public String getUserId(String socketId) {
        return socketIdToUserId.get(socketId);
    }
    
    /**
     * Gets the socketId for a given userId.
     * 
     * @param userId the user identifier
     * @return the socketId if found, null otherwise
     */
    public String getSocketId(String userId) {
        return userIdToSocketId.get(userId);
    }
    
    /**
     * Checks if a socket is authenticated.
     * 
     * @param socketId the socket identifier
     * @return true if the socket is authenticated, false otherwise
     */
    public boolean isAuthenticated(String socketId) {
        return authenticatedSockets.contains(socketId);
    }
}

