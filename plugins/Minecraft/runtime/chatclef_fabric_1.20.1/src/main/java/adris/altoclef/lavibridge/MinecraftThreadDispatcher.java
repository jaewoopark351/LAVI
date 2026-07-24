package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this class to keep LAVI bridge work on the Minecraft client thread.

import net.minecraft.client.MinecraftClient;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MinecraftThreadDispatcher {

    private static final long DEFAULT_TIMEOUT_MILLIS = 3000L;

    public <T> T call(Callable<T> action) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null) {
            throw new IllegalStateException("Minecraft client is not available.");
        }

        client.execute(() -> {
            try {
                future.complete(action.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        try {
            return future.get(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw new IllegalStateException("Timed out waiting for the Minecraft client thread.", timeoutException);
        }
    }

    public void run(Runnable action) throws Exception {
        call(() -> {
            action.run();
            return null;
        });
    }
}
