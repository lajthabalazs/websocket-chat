package ca.lajtha.websocketchat.server.http;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Controller
public class ErrorController {
    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);
    
    @Error(status = HttpStatus.NOT_FOUND)
    @Produces(MediaType.TEXT_HTML)
    public HttpResponse<String> notFound(HttpRequest<?> request) {
        String content = loadHtmlFromResources("404.html");
        return HttpResponse.<String>status(HttpStatus.NOT_FOUND, content).contentType(MediaType.TEXT_HTML);
    }
    
    private String loadHtmlFromResources(String filename) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                logger.warn("Warning: {} not found in resources, using fallback HTML", filename);
                return "<html><body><h1>404 Not Found</h1><p>The requested resource was not found.</p><a href=\"/\">Go to Home</a></body></html>";
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            logger.error("Error loading {} from resources", filename, e);
            return "<html><body><h1>404 Not Found</h1><p>The requested resource was not found.</p><a href=\"/\">Go to Home</a></body></html>";
        }
    }
}

