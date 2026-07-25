package adris.altoclef.lavibridge.routes;

//20260725_kpopmodder: Added command/action LAVI bridge route registration.

import adris.altoclef.lavibridge.LaviCommandAdapter;
import adris.altoclef.lavibridge.http.LaviHttpExchangeHandler;
import com.sun.net.httpserver.HttpServer;

public class LaviActionRoutes {

    private final LaviHttpExchangeHandler exchangeHandler;
    private final LaviCommandAdapter commandAdapter;

    public LaviActionRoutes(
            LaviHttpExchangeHandler exchangeHandler,
            LaviCommandAdapter commandAdapter
    ) {
        this.exchangeHandler = exchangeHandler;
        this.commandAdapter = commandAdapter;
    }

    public void register(HttpServer server) {
        server.createContext("/v1/actions/get-item", exchange -> exchangeHandler.handlePost(
                exchange,
                commandAdapter::getItem
        ));
        server.createContext("/v1/actions/goto", exchange -> exchangeHandler.handlePost(
                exchange,
                commandAdapter::gotoTarget
        ));
        server.createContext("/v1/actions/equip", exchange -> exchangeHandler.handlePost(
                exchange,
                commandAdapter::equip
        ));
        server.createContext("/v1/actions/get-and-equip", exchange -> exchangeHandler.handlePost(
                exchange,
                commandAdapter::getAndEquip
        ));
        server.createContext("/v1/actions/craft", exchange -> exchangeHandler.handlePost(
                exchange,
                commandAdapter::craft
        ));
        server.createContext("/v1/actions/stop", exchange -> exchangeHandler.handlePost(
                exchange,
                ignored -> commandAdapter.stop()
        ));
    }
}
