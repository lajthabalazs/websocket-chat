package ca.lajtha.websocketchat.server.http;

import ca.lajtha.websocketchat.server.http.dto.*;
import ca.lajtha.websocketchat.user.UserToken;
import ca.lajtha.websocketchat.user.UserManager;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.Cookies;
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
            
            // Set HTTP-only cookie with JWT token
            Cookie authCookie = Cookie.of("authToken", loginResponse.token())
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .maxAge(7 * 24 * 60 * 60); // 7 days
            
            return HttpResponse.ok(new LoginResponse(null, loginResponse.userId()))
                    .cookie(authCookie);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Checks if the user is authenticated by verifying the HTTP-only cookie.
     * Returns user info without exposing the token.
     * GET /auth/me
     */
    @Get("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> getCurrentUser(Cookies cookies) {
        Cookie authCookie = cookies.findCookie("authToken").orElse(null);
        
        if (authCookie == null || authCookie.getValue() == null || authCookie.getValue().isEmpty()) {
            return HttpResponse.unauthorized().body(Map.of("error", "Not authenticated"));
        }
        
        String token = authCookie.getValue();
        
        // Verify token is valid by extracting userId
        String userId = userManager.getUserIdFromToken(token);
        if (userId == null || !userManager.validateToken(userId, token)) {
            // Invalid token, clear the cookie
            Cookie clearCookie = Cookie.of("authToken", "")
                    .httpOnly(true)
                    .maxAge(0);
            return HttpResponse.unauthorized()
                    .cookie(clearCookie)
                    .body(Map.of("error", "Invalid or expired token"));
        }
        
        // Return user info without exposing the token
        return HttpResponse.ok(Map.of("userId", userId, "authenticated", true));
    }
    
    /**
     * Logs out the user by clearing the authentication cookie.
     * POST /auth/logout
     */
    @Post("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> logout() {
        // Clear the authentication cookie
        Cookie clearCookie = Cookie.of("authToken", "")
                .httpOnly(true)
                .maxAge(0);
        
        return HttpResponse.ok(Map.of("message", "Logged out successfully"))
                .cookie(clearCookie);
    }
}

