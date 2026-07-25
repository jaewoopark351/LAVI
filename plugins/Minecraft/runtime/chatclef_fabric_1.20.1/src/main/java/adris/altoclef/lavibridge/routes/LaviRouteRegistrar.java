package adris.altoclef.lavibridge.routes;

//20260725_kpopmodder: Added this registrar to keep HTTP route wiring out of LaviBridgeServer.

import adris.altoclef.lavibridge.LaviActionRegistry;
import adris.altoclef.lavibridge.LaviCommandAdapter;
import adris.altoclef.lavibridge.LaviStateReader;
import adris.altoclef.lavibridge.MinecraftThreadDispatcher;
import adris.altoclef.lavibridge.http.LaviHttpExchangeHandler;
import com.sun.net.httpserver.HttpServer;

public class LaviRouteRegistrar {

    private final LaviReadRoutes readRoutes;
    private final LaviActionRoutes actionRoutes;

    public LaviRouteRegistrar(
            MinecraftThreadDispatcher dispatcher,
            LaviStateReader stateReader,
            LaviActionRegistry actionRegistry,
            LaviCommandAdapter commandAdapter
    ) {
        LaviHttpExchangeHandler exchangeHandler = new LaviHttpExchangeHandler();
        this.readRoutes = new LaviReadRoutes(exchangeHandler, dispatcher, stateReader, actionRegistry);
        this.actionRoutes = new LaviActionRoutes(exchangeHandler, commandAdapter);
    }

    public void register(HttpServer server) {
        readRoutes.register(server);
        actionRoutes.register(server);
    }
}
