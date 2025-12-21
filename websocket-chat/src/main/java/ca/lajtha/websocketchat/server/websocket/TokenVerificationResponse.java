package ca.lajtha.websocketchat.server.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response sent to the client after token verification.
 */
public class TokenVerificationResponse {
    public static final String MESSAGE_TYPE = "tokenVerificationResponse";
    
    @JsonProperty("type")
    private final String type;
    
    @JsonProperty("success")
    private final boolean success;
    
    @JsonProperty("message")
    private final String message;
    
    private TokenVerificationResponse(String type, boolean success, String message) {
        this.type = type;
        this.success = success;
        this.message = message;
    }
    
    public static TokenVerificationResponse success() {
        return new TokenVerificationResponse(MESSAGE_TYPE, true, "Token verified successfully");
    }
    
    public static TokenVerificationResponse failure(String message) {
        return new TokenVerificationResponse(MESSAGE_TYPE, false, message);
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
}
