package adris.altoclef.eventbus;

import java.util.function.Consumer;
import lavi.minecraft.diagnostics.container.store.deposit.counter.StoreCounterIdentity;
import lavi.minecraft.diagnostics.container.store.deposit.counter.StoreCounterTrace;

// A wrapper object for event subscription
public class Subscription<T> {
    private final Consumer<T> callback;
    private boolean shouldDelete;
    //20260914_kpopmodder: Stable local observation identity and an optional frozen owner trace; never routing state.
    private final long diagnosticId = StoreCounterIdentity.next();
    private StoreCounterTrace diagnosticCounterTrace = StoreCounterTrace.NOOP;

    public long diagnosticId() { return diagnosticId; }
    public StoreCounterTrace diagnosticCounterTrace() { return diagnosticCounterTrace; }
    void diagnosticCounterTrace(StoreCounterTrace trace) {
        diagnosticCounterTrace = trace == null ? StoreCounterTrace.NOOP : trace;
    }

    public Subscription(Consumer<T> callback) {
        this.callback = callback;
    }

    public void accept(T event) {
        callback.accept(event);
    }

    public void delete() {
        shouldDelete = true;
    }

    public boolean shouldDelete() {
        return shouldDelete;
    }

    //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
    public String diagnosticCallbackClassName() {
        try {
            return callback == null ? "unavailable" : callback.getClass().getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }
}
