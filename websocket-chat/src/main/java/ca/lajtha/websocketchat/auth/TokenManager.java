package ca.lajtha.websocketchat.auth;

/**
 * Interface for managing JWT token verification and user identification.
 * Implementations should verify JWT tokens and extract user IDs from them.
 */
public interface TokenManager {
    /**
     * Verifies a JWT token and extracts the user ID from it.
     * 
     * @param token the JWT token to verify
     * @return the user ID if the token is valid, null otherwise
     */
    String getUserIdFromToken(String token);
}

