package ca.lajtha.websocketchat.server.http.dto;

/**
 * Request DTO for joining a game.
 */
public class JoinGameRequest {
    private String playerId;
    private String gameId;
    
    public JoinGameRequest() {
    }
    
    public JoinGameRequest(String playerId, String gameId) {
        this.playerId = playerId;
        this.gameId = gameId;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
}





