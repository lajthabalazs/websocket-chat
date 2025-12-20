package ca.lajtha.websocketchat.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // Handle HTTP request
        String uri = request.uri();
        HttpMethod method = request.method();
        
        System.out.println("Received HTTP " + method + " request: " + uri);
        
        // Create response
        FullHttpResponse response;
        
        if (method == HttpMethod.GET && "/".equals(uri)) {
            // Handle root path - load HTML from resources
            String content = loadHtmlFromResources("index.html");
            response = createResponse(OK, content);
        } else if (method == HttpMethod.GET && "/health".equals(uri)) {
            // Handle health check
            String content = "{\"status\":\"ok\"}";
            response = createResponse(OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        } else {
            // Handle 404
            String content = "<html><body><h1>404 Not Found</h1><p>The requested resource was not found.</p></body></html>";
            response = createResponse(NOT_FOUND, content);
        }
        
        // Send response
        ctx.writeAndFlush(response);
    }

    private FullHttpResponse createResponse(HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                status,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        
        return response;
    }

    private String loadHtmlFromResources(String filename) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                System.err.println("Warning: " + filename + " not found in resources, using fallback HTML");
                return "<html><body><h1>HTTP Server</h1><p>Server is running!</p></body></html>";
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            System.err.println("Error loading " + filename + " from resources: " + e.getMessage());
            return "<html><body><h1>HTTP Server</h1><p>Server is running!</p><p>Error loading HTML file.</p></body></html>";
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("HTTP Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}

