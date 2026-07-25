package adris.altoclef.lavibridge.http;

//20260725_kpopmodder: Added this writer to isolate JSON response serialization.

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class LaviHttpResponseWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void writeJson(HttpExchange exchange, int statusCode, Map<String, Object> body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
        }
    }
}
