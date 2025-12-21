package ca.lajtha.websocketchat.server.http;

import ca.lajtha.websocketchat.server.http.dto.*;
import ca.lajtha.websocketchat.user.UserToken;
import ca.lajtha.websocketchat.user.UserManager;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Controller for authentication endpoints (login and registration).
 */
@Controller("/auth")
public class AuthController {
    
    private final UserManager userManager;
    
    @Inject
    public AuthController(UserManager userManager) {
        this.userManager = userManager;
    }
    
    /**
     * Registers a new user.
     * POST /auth/register
     * Body: {"email": "user@example.com", "password": "password123"}
     */
    @Post("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> register(@Body RegisterRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "email is required"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "password is required"));
            }
            
            String userId = userManager.register(request.getEmail(), request.getPassword());
            if (userId == null) {
                return HttpResponse.badRequest(Map.of("error", "email already exists"));
            }
            
            return HttpResponse.ok(new RegisterResponse(userId, "User registered successfully"));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Logs in a user and returns a JWT token.
     * POST /auth/login
     * Body: {"email": "user@example.com", "password": "password123"}
     */
    @Post("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse<?> login(@Body LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "email is required"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "password is required"));
            }
            
            UserToken loginResponse = userManager.login(request.getEmail(), request.getPassword());
            if (loginResponse == null) {
                return HttpResponse.unauthorized().body(Map.of("error", "Invalid email or password"));
            }
            
            return HttpResponse.ok(new LoginResponse(loginResponse.token(), loginResponse.userId()));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }
}

