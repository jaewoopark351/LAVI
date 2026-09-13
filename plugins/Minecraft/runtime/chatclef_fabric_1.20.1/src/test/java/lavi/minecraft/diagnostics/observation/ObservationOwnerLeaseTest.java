package lavi.minecraft.diagnostics.observation;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: OFF cannot overtake an in-flight pin and late callbacks cannot repopulate its ledger.
final class ObservationOwnerLeaseTest {
    @Test void pinWriteAndOffInvalidationShareOneLease() throws Exception {
        boolean enabled = ChatClefDiagnostics.isBoundaryEnabled();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch formatting = new CountDownLatch(1), release = new CountDownLatch(1), offStarted = new CountDownLatch(1);
        Object instance = new Object(), world = new Object();
        try {
            ChatClefDiagnostics.setBoundaryEnabled(true);
            ObservationScope scope = ObservationDiagnostics.open(ObservationDiagnostics.captureActivation(instance, world),
                    "mining", "lease-test-operation");
            assertTrue(scope.isCurrent());
            Number paused = new Number() {
                public int intValue() { return 1; }
                public long longValue() { return 1; }
                public float floatValue() { return 1; }
                public double doubleValue() { return 1; }
                public String toString() {
                    formatting.countDown();
                    try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("pin never released"); }
                    catch (InterruptedException e) { throw new AssertionError(e); }
                    return "frozen";
                }
            };
            Future<?> pin = workers.submit(() -> scope.pin("first", "value", paused));
            assertTrue(formatting.await(5, TimeUnit.SECONDS));
            Future<?> off = workers.submit(() -> { offStarted.countDown(); ChatClefDiagnostics.setBoundaryEnabled(false); });
            assertTrue(offStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> off.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            pin.get(5, TimeUnit.SECONDS);
            off.get(5, TimeUnit.SECONDS);
            Object[] frozen = scope.ledger().summary();
            Object[] registry = ObservationDiagnostics.lifecycleObserver().finalSnapshotFields();
            scope.pin("late", "value", "must-not-appear");
            ObservationDiagnostics.captureFailed("late-worker", "must-not-appear");
            assertArrayEquals(frozen, scope.ledger().summary());
            assertArrayEquals(registry, ObservationDiagnostics.lifecycleObserver().finalSnapshotFields());
            ChatClefDiagnostics.setBoundaryEnabled(true);
            scope.pin("old-generation", "value", "must-not-appear");
            assertArrayEquals(frozen, scope.ledger().summary());
        } finally {
            release.countDown();
            workers.shutdownNow();
            ChatClefDiagnostics.setBoundaryEnabled(enabled);
        }
    }
}
