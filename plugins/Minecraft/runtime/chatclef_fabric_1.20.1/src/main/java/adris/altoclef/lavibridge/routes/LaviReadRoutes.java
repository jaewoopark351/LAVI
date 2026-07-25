package adris.altoclef.lavibridge.routes;

//20260725_kpopmodder: Added read-only LAVI bridge route registration.

import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.LaviStateReader;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;
import adris.altoclef.lavibridge.http.LaviHttpExchangeHandler;
import com.sun.net.httpserver.HttpServer;

public class LaviReadRoutes {

    private final LaviHttpExchangeHandler exchangeHandler;
    private final MinecraftThreadDispatcher dispatcher;
    private final LaviStateReader stateReader;
    private final LaviActionRegistry actionRegistry;

    public LaviReadRoutes(
            LaviHttpExchangeHandler exchangeHandler,
            MinecraftThreadDispatcher dispatcher,
            LaviStateReader stateReader,
            LaviActionRegistry actionRegistry
    ) {
        this.exchangeHandler = exchangeHandler;
        this.dispatcher = dispatcher;
        this.stateReader = stateReader;
        this.actionRegistry = actionRegistry;
    }

    public void register(HttpServer server) {
        server.createContext("/v1/health", exchange -> exchangeHandler.handleGet(
                exchange,
                () -> dispatcher.call(stateReader::health)
        ));
        server.createContext("/v1/status", exchange -> exchangeHandler.handleGet(
                exchange,
                () -> dispatcher.call(stateReader::status)
        ));
        server.createContext("/v1/inventory", exchange -> exchangeHandler.handleGet(
                exchange,
                () -> dispatcher.call(stateReader::inventory)
        ));
        server.createContext("/v1/actions/current", exchange -> exchangeHandler.handleGet(
                exchange,
                actionRegistry::currentAction
        ));
    }
}
