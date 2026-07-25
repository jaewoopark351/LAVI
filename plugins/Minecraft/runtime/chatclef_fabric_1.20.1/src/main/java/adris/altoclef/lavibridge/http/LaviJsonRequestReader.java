package adris.altoclef.lavibridge.http;

//20260725_kpopmodder: Added this reader to isolate JSON request body parsing.

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class LaviJsonRequestReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (body.isEmpty()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid JSON request body.", exception);
        }
    }
}
