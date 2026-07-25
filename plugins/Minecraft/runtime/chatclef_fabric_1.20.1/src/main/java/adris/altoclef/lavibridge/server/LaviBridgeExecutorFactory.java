package adris.altoclef.lavibridge.server;

//20260725_kpopmodder: Added this factory to keep HTTP worker thread setup separate from server lifecycle.

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LaviBridgeExecutorFactory {

    public ExecutorService create() {
        return Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "lavi-bridge-http");
            thread.setDaemon(true);
            return thread;
        });
    }
}
