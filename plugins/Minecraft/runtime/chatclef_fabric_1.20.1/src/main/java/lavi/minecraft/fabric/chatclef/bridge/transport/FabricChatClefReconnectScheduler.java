package lavi.minecraft.fabric.chatclef.bridge.transport;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//20260801_kpopmodder: Keep Fabric ChatClef reconnect timing out of the WebSocket listener.
public final class FabricChatClefReconnectScheduler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LAVI-Fabric-ChatClef-Bridge-Reconnect");
        thread.setDaemon(true);
        return thread;
    });

    public void runNow(Runnable action) {
        executor.execute(action);
    }

    public void runLater(Runnable action, Duration delay) {
        executor.schedule(action, Math.max(0L, delay.toMillis()), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }
}
