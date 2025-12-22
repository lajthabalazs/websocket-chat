package ca.lajtha.websocketchat.user;

import ca.lajtha.websocketchat.PropertiesLoader;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Properties;

/**
 * Manages JWT token operations including extraction and validation of user IDs.
 * Handles token parsing, validation, and user ID extraction from JWT tokens.
 */
@Singleton
public class TokenManager {
    private final Algorithm jwtAlgorithm;
    private final PropertiesLoader propertiesLoader;
    
    // JWT configuration
    private static final String USER_ID_CLAIM = "userId";
    private static final String DEFAULT_JWT_SECRET = "your-secret-key-change-in-production";
    
    /**
     * Creates a new TokenManager with the specified properties loader.
     * 
     * @param propertiesLoader the PropertiesLoader for loading JWT configuration
     */
    @Inject
    public TokenManager(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
        String jwtSecret = loadJwtSecret();
        this.jwtAlgorithm = Algorithm.HMAC256(jwtSecret);
    }
    
    /**
     * Loads the JWT secret key from server.properties file.
     * 
     * @return the JWT secret key, or a default value if not found
     */
    private String loadJwtSecret() {
        Properties props = propertiesLoader.loadProperties();
        String secret = propertiesLoader.getProperty(props, "jwt.secret", DEFAULT_JWT_SECRET);
        if (DEFAULT_JWT_SECRET.equals(secret)) {
            System.err.println("Warning: Using default JWT secret. Please change jwt.secret in server.properties for production!");
        }
        return secret;
    }
    
    /**
     * Extracts the user ID from a JWT token after validating it.
     * This method validates the token signature and expiration before extracting the user ID.
     * 
     * @param token the JWT token to extract user ID from
     * @return the user ID from the token if valid, null if token is invalid, expired, or doesn't contain userId
     */
    public String extractUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            DecodedJWT decodedJWT = JWT.require(jwtAlgorithm)
                    .build()
                    .verify(token.trim());
            
            return decodedJWT.getClaim(USER_ID_CLAIM).asString();
        } catch (JWTVerificationException e) {
            // Token is invalid, expired, or malformed
            return null;
        }
    }
    
    /**
     * Extracts the user ID from a JWT token without validating it.
     * Use this method when you only need to read the userId from a token without signature verification.
     * Note: This method does not verify the token signature or expiration.
     * 
     * @param token the JWT token
     * @return the userId from the token, or null if token is invalid or doesn't contain userId
     */
    public String extractUserIdWithoutValidation(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            DecodedJWT decodedJWT = JWT.decode(token.trim());
            return decodedJWT.getClaim(USER_ID_CLAIM).asString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Validates a JWT token and verifies it contains the specified userId.
     * 
     * @param userId the unique user identifier to verify
     * @param token the JWT token to validate
     * @return true if the token is valid and contains the matching userId, false otherwise
     */
    public boolean validateToken(String userId, String token) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            DecodedJWT decodedJWT = JWT.require(jwtAlgorithm)
                    .build()
                    .verify(token.trim());
            
            String tokenUserId = decodedJWT.getClaim(USER_ID_CLAIM).asString();
            return userId.trim().equals(tokenUserId);
        } catch (JWTVerificationException e) {
            // Token is invalid, expired, or malformed
            return false;
        }
    }
    
    /**
     * Checks if a JWT token is valid (signature and expiration).
     * 
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            JWT.require(jwtAlgorithm)
                    .build()
                    .verify(token.trim());
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}

