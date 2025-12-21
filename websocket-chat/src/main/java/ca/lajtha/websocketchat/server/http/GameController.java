package ca.lajtha.websocketchat.server.http;

import ca.lajtha.websocketchat.game.GameInfo;
import ca.lajtha.websocketchat.game.GameManager;
import ca.lajtha.websocketchat.server.http.dto.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/games")
public class GameController {
    
    private final GameManager gameManager;
    
    @Inject
    public GameController(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * Creates a new game.
     * POST /games
     * Body: {"playerId": "player1", "gameParameters": {"name": "My Game"}}
     */
    @Post
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> createGame(@Body CreateGameRequest request) {
        try {
            if (request.getPlayerId() == null || request.getPlayerId().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "playerId is required"));
            }
            
            String gameId = gameManager.createGame(request.getPlayerId(), request.getGameParameters());
            return HttpResponse.ok(new CreateGameResponse(gameId));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Joins a player to a game.
     * POST /games/join
     * Body: {"playerId": "player1", "gameId": "game-1"}
     */
    @Post("/join")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> joinGame(@Body JoinGameRequest request) {
        try {
            if (request.getPlayerId() == null || request.getPlayerId().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "playerId is required"));
            }
            if (request.getGameId() == null || request.getGameId().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "gameId is required"));
            }
            
            gameManager.joinGame(request.getPlayerId(), request.getGameId());
            return HttpResponse.ok(Map.of("status", "joined", "gameId", request.getGameId()));
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Lists all available games.
     * GET /games
     */
    @Get
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<List<GameDescription>> listGames() {
        List<GameInfo> games = gameManager.listGames();
        List<GameDescription> descriptions = games.stream()
            .map(gameInfo -> new GameDescription(
                gameInfo.getGameId(),
                gameInfo.getName(),
                gameInfo.getCreatorId(),
                gameInfo.getCreatedAt()
            ))
            .collect(Collectors.toList());
        return HttpResponse.ok(descriptions);
    }
    
    /**
     * Stops a game.
     * DELETE /games/{gameId}
     */
    @Delete("/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> stopGame(String gameId) {
        try {
            if (gameId == null || gameId.trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "gameId is required"));
            }
            
            gameManager.stopGame(gameId);
            return HttpResponse.ok(Map.of("status", "stopped", "gameId", gameId));
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }
}

