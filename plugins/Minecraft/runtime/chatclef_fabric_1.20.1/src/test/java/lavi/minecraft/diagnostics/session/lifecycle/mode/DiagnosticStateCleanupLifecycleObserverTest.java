package lavi.minecraft.diagnostics.session.lifecycle.mode;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260831_kpopmodder: Verify cleanup runs at both state-invalidating session boundaries.
class DiagnosticStateCleanupLifecycleObserverTest {
    @Test
    void cleanupRunsBeforeOffAndAfterCleanTeardownRegardlessOfSinkOutcome() {
        AtomicInteger clearCalls = new AtomicInteger();
        DiagnosticStateCleanupLifecycleObserver observer =
                new DiagnosticStateCleanupLifecycleObserver(clearCalls::incrementAndGet);

        observer.beforeModeOff();
        observer.afterCleanTeardownSnapshotAttempt(false);
        observer.afterCleanTeardownSnapshotAttempt(true);

        assertEquals(3, clearCalls.get());
    }
}
