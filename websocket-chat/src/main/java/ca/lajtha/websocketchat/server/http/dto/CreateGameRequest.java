package ca.lajtha.websocketchat.server.http.dto;

import java.util.Map;

/**
 * Request DTO for creating a game.
 */
public class CreateGameRequest {
    private String playerId;
    private Map<String, Object> gameParameters;
    
    public CreateGameRequest() {
    }
    
    public CreateGameRequest(String playerId, Map<String, Object> gameParameters) {
        this.playerId = playerId;
        this.gameParameters = gameParameters;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public Map<String, Object> getGameParameters() {
        return gameParameters;
    }
    
    public void setGameParameters(Map<String, Object> gameParameters) {
        this.gameParameters = gameParameters;
    }
}

