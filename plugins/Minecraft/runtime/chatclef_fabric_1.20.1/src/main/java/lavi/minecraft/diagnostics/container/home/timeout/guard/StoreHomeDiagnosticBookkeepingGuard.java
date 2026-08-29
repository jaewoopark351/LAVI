package lavi.minecraft.diagnostics.container.home.timeout.guard;

//20260829_kpopmodder: Isolate failures produced only by diagnostic bookkeeping.
public final class StoreHomeDiagnosticBookkeepingGuard {
    private StoreHomeDiagnosticBookkeepingGuard() {
    }

    public static void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError ignored) {
            // This action must contain diagnostic-only bookkeeping, never engine reads.
        }
    }
}
