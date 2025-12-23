package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.user.TokenManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AttributeKey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles WebSocket handshake authentication by reading the authToken cookie
 * from the HTTP upgrade request and validating it before the WebSocket connection is established.
 */
public class WebSocketHandshakeHandler extends ChannelInboundHandlerAdapter {
    private static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");
    private static final Pattern COOKIE_PATTERN = Pattern.compile("authToken=([^;\\s]+)");
    
    private final TokenManager tokenManager;
    
    public WebSocketHandshakeHandler(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            
            System.out.println("Received HTTP request: " + request.method() + " " + request.uri() + " from " + ctx.channel().remoteAddress());
            System.out.println("Upgrade header: " + request.headers().get(HttpHeaderNames.UPGRADE));
            System.out.println("Connection header: " + request.headers().get(HttpHeaderNames.CONNECTION));
            
            // Check if this is a WebSocket upgrade request
            String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                System.out.println("WebSocket upgrade request detected");
                // Try to extract token from cookie first
                String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
                if (cookieHeader != null) {
                    // Check if authToken cookie is present (without logging the full token)
                    boolean hasAuthToken = cookieHeader.contains("authToken=");
                    System.out.println("Cookie header present: " + (hasAuthToken ? "yes (contains authToken)" : "yes (no authToken)"));
                } else {
                    System.out.println("Cookie header: null");
                }
                String token = extractTokenFromCookie(cookieHeader);
                System.out.println("Token from cookie: " + (token != null ? "found" : "not found"));
                
                // If no token in cookie, try query parameter (for cross-port connections)
                if (token == null || token.isEmpty()) {
                    String uri = request.uri();
                    System.out.println("Checking query parameter in URI: " + uri);
                    token = extractTokenFromQuery(uri);
                    System.out.println("Token from query: " + (token != null ? "found" : "not found"));
                }
                
                if (token == null || token.isEmpty()) {
                    // No token found, reject the handshake
                    System.err.println("WebSocket handshake rejected: No authToken found in cookie or query parameter");
                    ctx.writeAndFlush(createUnauthorizedResponse(request));
                    return;
                }
                
                // Validate token and extract userId
                String userId = tokenManager.extractUserId(token);
                
                if (userId == null) {
                    // Invalid token, reject the handshake
                    System.err.println("WebSocket handshake rejected: Invalid or expired token");
                    ctx.writeAndFlush(createUnauthorizedResponse(request));
                    return;
                }
                
                // Store userId in channel attributes for later use
                ctx.channel().attr(USER_ID_KEY).set(userId);
                System.out.println("WebSocket handshake authenticated for userId: " + userId);
                System.out.println("Passing request to WebSocketServerProtocolHandler for handshake completion...");
                
                // Retain the request reference before passing to WebSocketServerProtocolHandler
                // This is important when using HttpObjectAggregator
                request.retain();
            }
        }
        
        // Pass the message to the next handler (WebSocketServerProtocolHandler)
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Exception in WebSocketHandshakeHandler: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
    
    /**
     * Extracts the authToken value from the Cookie header.
     */
    private String extractTokenFromCookie(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }
        
        Matcher matcher = COOKIE_PATTERN.matcher(cookieHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extracts the authToken value from the query string (e.g., ?token=...).
     */
    private String extractTokenFromQuery(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        
        int queryIndex = uri.indexOf('?');
        if (queryIndex == -1) {
            return null;
        }
        
        String query = uri.substring(queryIndex + 1);
        String[] params = query.split("&");
        
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6); // "token=".length()
            }
        }
        
        return null;
    }
    
    /**
     * Creates an HTTP 401 Unauthorized response to reject the handshake.
     */
    private FullHttpResponse createUnauthorizedResponse(FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(),
                HttpResponseStatus.UNAUTHORIZED
        );
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        HttpUtil.setKeepAlive(response, false);
        return response;
    }
    
    /**
     * Gets the userId attribute key for accessing stored userId in channel attributes.
     */
    public static AttributeKey<String> getUserIdKey() {
        return USER_ID_KEY;
    }
}

