package ca.lajtha.websocketchat.auth;

/**
 * Stub implementation of TokenManager for development/testing.
 * This implementation will be replaced with a real JWT token manager later.
 * 
 * For now, it accepts any non-empty token and returns a userId based on the token.
 * In a real implementation, this would verify the JWT signature and extract the userId from the token claims.
 */
public class StubTokenManager implements TokenManager {
    
    @Override
    public String getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // Stub implementation: for now, just return the token as userId
        // TODO: Replace with actual JWT verification
        // This should:
        // 1. Verify the JWT signature
        // 2. Check token expiration
        // 3. Extract userId from token claims (e.g., "sub" or "userId" claim)
        return token;
    }
}

