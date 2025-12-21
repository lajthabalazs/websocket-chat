package ca.lajtha.websocketchat.game;

import ca.lajtha.websocketchat.server.websocket.MessageSender;
import ca.lajtha.websocketchat.server.websocket.*;
import com.google.inject.Inject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles control messages related to authentication and authorization.
 */
public class ConnectionManager implements MessageListener, PlayerMessageSender {
    private final Map<String, String> socketIdToUserId = new ConcurrentHashMap<>();
    private final Map<String, String> userIdToSocketId = new ConcurrentHashMap<>();
    private final Set<String> authenticatedSockets = ConcurrentHashMap.newKeySet();
    private Game game;
    private final MessageSender messageSender;

    @Inject
    public ConnectionManager(MessageSender messageSender) {
        this.messageSender = messageSender;
    }


    /**
     * Sets the game instance.
     *
     * @param game the game instance to set
     */
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void playerConnected(String socketId) {
        // TODO do nothing for now
        // Authentication happens at WebSocket handshake level
        // The socketId-to-userId mapping should be set when the connection is authenticated
    }
    
    /**
     * Authenticates a socket with a userId.
     * This method is used to establish the mapping between socketId and userId.
     * 
     * @param socketId the socket identifier
     * @param userId the user identifier
     */
    public void authenticateSocket(String socketId, String userId) {
        if (socketId == null || userId == null) {
            return;
        }
        
        // If this socket was already authenticated with a different userId, disconnect the old user
        String oldUserId = socketIdToUserId.get(socketId);
        if (oldUserId != null && !oldUserId.equals(userId)) {
            userIdToSocketId.remove(oldUserId);
            if (game != null) {
                game.handlePlayerDisconnected(oldUserId);
            }
        }
        
        // If this userId is already mapped to a different socket, remove the old mapping
        String oldSocketId = userIdToSocketId.get(userId);
        if (oldSocketId != null && !oldSocketId.equals(socketId)) {
            socketIdToUserId.remove(oldSocketId);
            authenticatedSockets.remove(oldSocketId);
        }
        
        // Establish new mappings
        socketIdToUserId.put(socketId, userId);
        userIdToSocketId.put(userId, socketId);
        authenticatedSockets.add(socketId);
        
        // Notify game of new connection
        if (game != null) {
            game.handlePlayerConnected(userId);
        }
    }

    @Override
    public void playerDisconnected(String socketId) {
        if(socketIdToUserId.containsKey(socketId)){
            var userId = socketIdToUserId.remove(socketId);
            userIdToSocketId.remove(userId);
            authenticatedSockets.remove(socketId);
            game.handlePlayerDisconnected(userId);
        }
    }

    @Override
    public void handlePlayerMessage(String socketId, String request) {
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
     * Sends a message to a specific player.
     * Accepts either a socketId or userId - if userId is provided, it will be translated to socketId.
     * 
     * @param playerId the unique identifier of the socket or user (can be socketId or userId)
     * @param message the message to send
     * @return true if the message was sent, false if the player is not connected
     */
    @Override
    public boolean sendToPlayer(String playerId, String message) {
        String socketId;
        
        // First check if playerId is a userId (look up in userIdToSocketId)
        socketId = userIdToSocketId.get(playerId);
        
        // If not found as userId, treat playerId as socketId
        // Note: We can't verify if socketId is valid without tracking all connected sockets,
        // so we'll attempt to send and let messageSender handle invalid sockets
        if (socketId == null) {
            socketId = playerId;
        }
        
        // Send the message (messageSender will handle if socket is valid)
        messageSender.sendMessage(socketId, message);
        return true;
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

