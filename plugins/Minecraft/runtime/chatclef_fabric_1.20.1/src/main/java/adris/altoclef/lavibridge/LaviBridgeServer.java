package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this local HTTP bridge so LAVI can inspect and command AltoClef safely.

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LaviBridgeServer {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 4316;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MinecraftThreadDispatcher dispatcher = new MinecraftThreadDispatcher();
    private final LaviActionRegistry actionRegistry = new LaviActionRegistry();
    private final LaviStateReader stateReader;
    private final LaviCommandAdapter commandAdapter;

    private HttpServer server;
    private ExecutorService executor;

    public LaviBridgeServer(AltoClef mod) {
        LaviStopController stopController = new LaviStopController(mod);
        stateReader = new LaviStateReader(mod, actionRegistry);
        commandAdapter = new LaviCommandAdapter(mod, dispatcher, actionRegistry, stopController);
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST), PORT), 0);
            server.createContext("/v1/health", exchange -> handleGet(exchange, () -> dispatcher.call(stateReader::health)));
            server.createContext("/v1/status", exchange -> handleGet(exchange, () -> dispatcher.call(stateReader::status)));
            server.createContext("/v1/inventory", exchange -> handleGet(exchange, () -> dispatcher.call(stateReader::inventory)));
            server.createContext("/v1/actions/current", exchange -> handleGet(exchange, actionRegistry::currentAction));
            server.createContext("/v1/actions/get-item", exchange -> handlePost(exchange, commandAdapter::getItem));
            server.createContext("/v1/actions/stop", exchange -> handlePost(exchange, ignored -> commandAdapter.stop()));

            executor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "lavi-bridge-http");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "lavi-bridge-shutdown"));
            Debug.logMessage("LAVI bridge listening on http://" + HOST + ":" + PORT);
        } catch (IOException exception) {
            server = null;
            Debug.logWarning("Could not start LAVI bridge: " + exception.getMessage());
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void handleGet(HttpExchange exchange, ThrowingSupplier<Map<String, Object>> supplier) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, error("method_not_allowed", "Use GET."));
            return;
        }
        handle(exchange, ignored -> supplier.get(), Map.of());
    }

    private void handlePost(HttpExchange exchange, ThrowingFunction<Map<String, Object>, Map<String, Object>> handler) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, error("method_not_allowed", "Use POST."));
            return;
        }

        Map<String, Object> body;
        try {
            body = readBody(exchange);
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, error("bad_request", exception.getMessage()));
            return;
        }
        handle(exchange, handler, body);
    }

    private void handle(HttpExchange exchange, ThrowingFunction<Map<String, Object>, Map<String, Object>> handler, Map<String, Object> body) throws IOException {
        try {
            writeJson(exchange, 200, handler.apply(body));
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, error("bad_request", exception.getMessage()));
        } catch (IllegalStateException exception) {
            writeJson(exchange, 409, error("conflict", exception.getMessage()));
        } catch (Exception exception) {
            writeJson(exchange, 500, error("internal_error", exception.getMessage()));
        }
    }

    private Map<String, Object> readBody(HttpExchange exchange) throws IOException {
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

    private void writeJson(HttpExchange exchange, int statusCode, Map<String, Object> body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
        }
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("error", code);
        result.put("message", message == null ? "" : message);
        return result;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T value) throws Exception;
    }
}
