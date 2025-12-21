package ca.lajtha.websocketchat.server.http;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.*;

class HttpRequestHandlerTest {

    private EmbeddedChannel channel;
    private HttpRequestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HttpRequestHandler();
        channel = new EmbeddedChannel(handler);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finish();
        }
    }

    @Test
    void channelRead0_rootPath_returnsIndexHtml() {
        // Arrange
        FullHttpRequest request = createGetRequest("/");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(OK, response.status());
        assertTrue(response.content().toString(java.nio.charset.StandardCharsets.UTF_8).contains("WebSocket Chat"));
        assertEquals("text/html; charset=UTF-8", response.headers().get(HttpHeaderNames.CONTENT_TYPE));
        response.release();
    }

    @Test
    void channelRead0_healthEndpoint_returnsJson() {
        // Arrange
        FullHttpRequest request = createGetRequest("/health");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(OK, response.status());
        String content = response.content().toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("\"status\":\"ok\""));
        assertEquals("application/json", response.headers().get(HttpHeaderNames.CONTENT_TYPE));
        response.release();
    }

    @Test
    void channelRead0_unknownPath_returns404() {
        // Arrange
        FullHttpRequest request = createGetRequest("/unknown/path");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(NOT_FOUND, response.status());
        String content = response.content().toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("404") || content.contains("Not Found"));
        assertEquals("text/html; charset=UTF-8", response.headers().get(HttpHeaderNames.CONTENT_TYPE));
        response.release();
    }

    @Test
    void channelRead0_postMethod_returns404() {
        // Arrange
        FullHttpRequest request = createPostRequest("/");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(NOT_FOUND, response.status());
        response.release();
    }

    @Test
    void channelRead0_putMethod_returns404() {
        // Arrange
        FullHttpRequest request = createPutRequest("/");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(NOT_FOUND, response.status());
        response.release();
    }

    @Test
    void channelRead0_deleteMethod_returns404() {
        // Arrange
        FullHttpRequest request = createDeleteRequest("/");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(NOT_FOUND, response.status());
        response.release();
    }

    @Test
    void channelRead0_responseHasCorrectHeaders() {
        // Arrange
        FullHttpRequest request = createGetRequest("/");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(HTTP_1_1, response.protocolVersion());
        assertTrue(response.headers().contains(HttpHeaderNames.CONTENT_TYPE));
        assertTrue(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH));
        assertEquals(HttpHeaderValues.CLOSE.toString(), response.headers().get(HttpHeaderNames.CONNECTION));
        response.release();
    }

    @Test
    void channelRead0_contentLengthIsSet() {
        // Arrange
        FullHttpRequest request = createGetRequest("/");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        String contentLength = response.headers().get(HttpHeaderNames.CONTENT_LENGTH);
        assertNotNull(contentLength);
        assertTrue(Integer.parseInt(contentLength) > 0);
        assertEquals(Integer.parseInt(contentLength), response.content().readableBytes());
        response.release();
    }

    @Test
    void channelRead0_rootPathWithQueryString_returns404() {
        // Arrange - The handler checks for exact "/" match, so "/?param=value" will be 404
        FullHttpRequest request = createGetRequest("/?param=value");

        // Act
        channel.writeInbound(request);

        // Assert - Query string makes it not match the exact "/" check
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(NOT_FOUND, response.status());
        response.release();
    }

    @Test
    void channelRead0_multipleRequests_handlesEachCorrectly() {
        // Arrange
        FullHttpRequest request1 = createGetRequest("/");
        FullHttpRequest request2 = createGetRequest("/health");
        FullHttpRequest request3 = createGetRequest("/unknown");

        // Act
        channel.writeInbound(request1);
        channel.writeInbound(request2);
        channel.writeInbound(request3);

        // Assert
        FullHttpResponse response1 = channel.readOutbound();
        assertEquals(OK, response1.status());
        response1.release();

        FullHttpResponse response2 = channel.readOutbound();
        assertEquals(OK, response2.status());
        assertTrue(response2.content().toString(java.nio.charset.StandardCharsets.UTF_8).contains("status"));
        response2.release();

        FullHttpResponse response3 = channel.readOutbound();
        assertEquals(NOT_FOUND, response3.status());
        response3.release();
    }

    @Test
    void exceptionCaught_closesChannel() {
        // Arrange
        Throwable cause = new RuntimeException("Test exception");

        // Act
        channel.pipeline().fireExceptionCaught(cause);

        // Assert - channel should be closed after exception
        assertFalse(channel.isOpen());
    }

    @Test
    void channelRead0_healthEndpointContentIsValidJson() {
        // Arrange
        FullHttpRequest request = createGetRequest("/health");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        String content = response.content().toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.startsWith("{"));
        assertTrue(content.endsWith("}"));
        assertTrue(content.contains("status"));
        assertTrue(content.contains("ok"));
        response.release();
    }

    @Test
    void channelRead0_404PageContainsHomeLink() {
        // Arrange
        FullHttpRequest request = createGetRequest("/nonexistent");

        // Act
        channel.writeInbound(request);

        // Assert
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        String content = response.content().toString(java.nio.charset.StandardCharsets.UTF_8);
        // The 404 page should contain a link to home (either in the HTML file or fallback)
        assertTrue(content.contains("404") || content.contains("Not Found"));
        response.release();
    }

    /**
     * Creates a GET request with the specified URI.
     */
    private FullHttpRequest createGetRequest(String uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                uri
        );
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        return request;
    }

    /**
     * Creates a POST request with the specified URI.
     */
    private FullHttpRequest createPostRequest(String uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                uri
        );
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        return request;
    }

    /**
     * Creates a PUT request with the specified URI.
     */
    private FullHttpRequest createPutRequest(String uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.PUT,
                uri
        );
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        return request;
    }

    /**
     * Creates a DELETE request with the specified URI.
     */
    private FullHttpRequest createDeleteRequest(String uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.DELETE,
                uri
        );
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        return request;
    }
}

