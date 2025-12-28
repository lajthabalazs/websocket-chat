package ca.lajtha.websocketchat.server.http.dto;

/**
 * Response DTO for creating a game.
 */
public class CreateGameResponse {
    private String gameId;
    
    public CreateGameResponse() {
    }
    
    public CreateGameResponse(String gameId) {
        this.gameId = gameId;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
}






