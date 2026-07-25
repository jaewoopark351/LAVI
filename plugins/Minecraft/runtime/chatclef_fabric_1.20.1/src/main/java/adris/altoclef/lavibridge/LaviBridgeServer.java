package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this local HTTP bridge so LAVI can inspect and command AltoClef safely.

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import com.sun.net.httpserver.HttpServer;
import adris.altoclef.lavibridge.routes.LaviRouteRegistrar;
import adris.altoclef.lavibridge.server.LaviBridgeExecutorFactory;
import adris.altoclef.lavibridge.server.LaviBridgeServerConfig;
import adris.altoclef.lavibridge.server.LaviHttpServerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class LaviBridgeServer {

    private final LaviBridgeServerConfig config = LaviBridgeServerConfig.localhostDefault();
    private final LaviHttpServerFactory serverFactory = new LaviHttpServerFactory();
    private final LaviBridgeExecutorFactory executorFactory = new LaviBridgeExecutorFactory();
    private final MinecraftThreadDispatcher dispatcher = new MinecraftThreadDispatcher();
    private final LaviActionRegistry actionRegistry = new LaviActionRegistry();
    private final LaviStateReader stateReader;
    private final LaviCommandAdapter commandAdapter;
    private final LaviRouteRegistrar routeRegistrar;

    private HttpServer server;
    private ExecutorService executor;

    public LaviBridgeServer(AltoClef mod) {
        LaviStopController stopController = new LaviStopController(mod);
        stateReader = new LaviStateReader(mod, actionRegistry);
        commandAdapter = new LaviCommandAdapter(mod, dispatcher, actionRegistry, stopController);
        routeRegistrar = new LaviRouteRegistrar(dispatcher, stateReader, actionRegistry, commandAdapter);
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }

        try {
            server = serverFactory.create(config);
            routeRegistrar.register(server);
            executor = executorFactory.create();
            server.setExecutor(executor);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "lavi-bridge-shutdown"));
            Debug.logMessage("LAVI bridge listening on " + config.baseUrl());
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
}
