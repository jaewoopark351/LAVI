package adris.altoclef.lavibridge.http;

//20260725_kpopmodder: Added this handler to centralize method checks and exception-to-HTTP mapping.

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class LaviHttpExchangeHandler {

    private final LaviJsonRequestReader requestReader;
    private final LaviHttpResponseWriter responseWriter;
    private final LaviHttpErrorFactory errorFactory;

    public LaviHttpExchangeHandler() {
        this(new LaviJsonRequestReader(), new LaviHttpResponseWriter(), new LaviHttpErrorFactory());
    }

    public LaviHttpExchangeHandler(
            LaviJsonRequestReader requestReader,
            LaviHttpResponseWriter responseWriter,
            LaviHttpErrorFactory errorFactory
    ) {
        this.requestReader = requestReader;
        this.responseWriter = responseWriter;
        this.errorFactory = errorFactory;
    }

    public void handleGet(HttpExchange exchange, LaviRouteSupplier<Map<String, Object>> supplier) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responseWriter.writeJson(exchange, 405, errorFactory.error("method_not_allowed", "Use GET."));
            return;
        }
        handle(exchange, ignored -> supplier.get(), Map.of());
    }

    public void handlePost(
            HttpExchange exchange,
            LaviRouteFunction<Map<String, Object>, Map<String, Object>> handler
    ) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responseWriter.writeJson(exchange, 405, errorFactory.error("method_not_allowed", "Use POST."));
            return;
        }

        Map<String, Object> body;
        try {
            body = requestReader.readBody(exchange);
        } catch (IllegalArgumentException exception) {
            responseWriter.writeJson(exchange, 400, errorFactory.error("bad_request", exception.getMessage()));
            return;
        }
        handle(exchange, handler, body);
    }

    private void handle(
            HttpExchange exchange,
            LaviRouteFunction<Map<String, Object>, Map<String, Object>> handler,
            Map<String, Object> body
    ) throws IOException {
        try {
            responseWriter.writeJson(exchange, 200, handler.apply(body));
        } catch (IllegalArgumentException exception) {
            responseWriter.writeJson(exchange, 400, errorFactory.error("bad_request", exception.getMessage()));
        } catch (IllegalStateException exception) {
            responseWriter.writeJson(exchange, 409, errorFactory.error("conflict", exception.getMessage()));
        } catch (Exception exception) {
            responseWriter.writeJson(exchange, 500, errorFactory.error("internal_error", exception.getMessage()));
        }
    }
}
