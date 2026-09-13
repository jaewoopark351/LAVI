package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Keep teardown ordering and prevent captured callbacks from reviving disabled state.
class DiagnosticOwnerRegistrationTest {
    @Test
    void retainsSnapshotBeforeTeardownAndRepeatedModeCleanupForRegisteredOwners() {
        DiagnosticSessionLifecycleRegistry registry = new DiagnosticSessionLifecycleRegistry();
        List<String> events = new ArrayList<>();
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("healthy", () -> events.add("disabled"));
        registry.registerOwner(owner, new DiagnosticSessionLifecycleObserver() {
            @Override public void beforeModeOff() { events.add("off"); }
            @Override public Object[] finalSnapshotFields() { events.add("snapshot"); return new Object[]{"healthy", 17}; }
            @Override public void afterCleanTeardownSnapshotAttempt(boolean returned) { events.add("teardown:" + returned); }
        });
        registry.notifyBeforeModeOff();
        Object[] fields = registry.finalSnapshotFields();
        registry.notifyAfterCleanTeardownSnapshotAttempt(false);
        registry.notifyBeforeModeOff();
        assertEquals(List.of("off", "snapshot", "teardown:false", "off"), events);
        assertEquals("healthy", fields[4]);
        assertTrue(owner.isAvailable());
    }

    @Test
    void disablingOneOwnerInvalidatesCapturedCallbacksWithoutTouchingOthers() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        AtomicInteger staleState = new AtomicInteger(4);
        AtomicInteger healthyCalls = new AtomicInteger();
        DiagnosticOwnerRegistration failed = new DiagnosticOwnerRegistration("failed", () -> staleState.set(0));
        DiagnosticOwnerRegistration healthy = new DiagnosticOwnerRegistration("healthy", () -> fail("Unrelated cleanup"));
        registry.registerOwner(failed, new DiagnosticSessionLifecycleObserver() {
            @Override public void beforeModeOff() { staleState.incrementAndGet(); }
            @Override public Object[] finalSnapshotFields() { staleState.incrementAndGet(); return new Object[]{"stale", 1}; }
        });
        registry.registerOwner(healthy, new DiagnosticSessionLifecycleObserver() {
            @Override public void beforeModeOff() { healthyCalls.incrementAndGet(); }
        });
        List<DiagnosticSessionLifecycleObserver> captured = registry.snapshot();
        failed.disable("FIXTURE");
        captured.forEach(DiagnosticSessionLifecycleObserver::beforeModeOff);
        assertEquals(0, captured.get(0).finalSnapshotFields().length);
        failed.runIfAvailable(() -> staleState.incrementAndGet());
        assertEquals(0, staleState.get());
        assertEquals(1, healthyCalls.get());
        assertTrue(healthy.isAvailable());
    }

    @Test
    void cleanupFailureDoesNotReenableOrEscapeRejectedOwner() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("broken-cleanup", () -> {
            throw new ExceptionInInitializerError("fixture");
        });
        assertDoesNotThrow(() -> registry.registerOwner(owner, (DiagnosticSessionLifecycleObserver) null));
        assertFalse(owner.isAvailable());
        assertDoesNotThrow(() -> owner.disable("again"));
    }

    @Test
    void cleanupWaitsForInFlightDiagnosticWriteAndLateWriteDoesNotRun() throws Exception {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        AtomicInteger state = new AtomicInteger();
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("serialized", () -> state.set(0));
        registry.registerOwner(owner, new DiagnosticSessionLifecycleObserver() { });
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        CountDownLatch disabling = new CountDownLatch(1);
        Thread writer = new Thread(() -> owner.runIfAvailable(() -> {
            entered.countDown();
            try { assertTrue(finish.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException failure) { throw new AssertionError(failure); }
            state.set(9);
        }));
        Thread disable = new Thread(() -> { disabling.countDown(); owner.disable("FIXTURE"); });
        writer.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        disable.start();
        assertTrue(disabling.await(5, TimeUnit.SECONDS));
        finish.countDown();
        writer.join(5_000);
        disable.join(5_000);
        assertFalse(writer.isAlive());
        assertFalse(disable.isAlive());
        owner.runIfAvailable(() -> state.set(10));
        assertEquals(0, state.get());
        assertFalse(owner.isAvailable());
    }

    @Test
    void originalCallbackFailureStillReachesExistingLifecycleFailureAccounting() {
        DiagnosticSessionLifecycleRegistry registry = new DiagnosticSessionLifecycleRegistry();
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("snapshot-failure", () -> { });
        registry.registerOwner(owner, new DiagnosticSessionLifecycleObserver() {
            @Override public Object[] finalSnapshotFields() { throw new IllegalStateException("fixture"); }
        });
        assertEquals(1, registry.finalSnapshotFields()[1]);
        assertTrue(owner.isAvailable());
    }
}
