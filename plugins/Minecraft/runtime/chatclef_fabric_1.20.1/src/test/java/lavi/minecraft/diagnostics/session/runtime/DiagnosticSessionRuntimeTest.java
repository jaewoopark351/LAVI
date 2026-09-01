package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.reset.DiagnosticSessionTestResetResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260831_kpopmodder: Verify mode transitions settle old-epoch emission leases before OFF returns.
class DiagnosticSessionRuntimeTest {
    @Test
    void offDispatchDoesNotMutateAdmissionOrSuppressionAccounting() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.OFF);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("off-session")
        );

        DiagnosticDispatchResult result = runtime.dispatch(
                DiagnosticEventFamily.ORDINARY_DETAIL,
                "OFF_EVENT",
                () -> {
                    throw new AssertionError("OFF must not invoke the physical emission");
                },
                ignored -> {
                    throw new AssertionError("OFF must not emit a cap event");
                }
        );

        assertFalse(result.admitted());
        DiagnosticSessionSnapshot snapshot = runtime.snapshot();
        assertEquals(0, snapshot.admittedSlots());
        assertEquals(0, snapshot.suppressedRequests());
        assertEquals(0, snapshot.emissionPending());
    }

    @Test
    void enabledToOffWaitsForTheAdmittedEmissionAndLeavesNoPendingLease() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY);
            DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                    mode,
                    new DiagnosticSessionAdmissionAuthority("race-session")
            );
            CountDownLatch emissionStarted = new CountDownLatch(1);
            CountDownLatch releaseEmission = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> emission = executor.submit(() -> runtime.dispatch(
                        DiagnosticEventFamily.ORDINARY_DETAIL,
                        "RACE_EVENT",
                        () -> {
                            emissionStarted.countDown();
                            await(releaseEmission);
                        },
                        ignored -> {
                        }
                ));
                emissionStarted.await();
                Future<?> disable = executor.submit(() -> runtime.setBoundaryEnabled(false));

                assertFalse(disable.isDone());
                assertTrue(mode.isBoundaryEnabled());
                releaseEmission.countDown();
                emission.get();
                disable.get();

                assertTrue(mode.isOff());
                assertEquals(0, runtime.snapshot().emissionPending());
                assertEquals(0, runtime.snapshot().emissionInProgress());
                assertEquals(1, runtime.snapshot().emissionCompleted());
            } finally {
                releaseEmission.countDown();
                executor.shutdownNow();
            }
        });
    }

    @Test
    void offOnlyIntegrationResetReplacesTheSessionWithoutAnEvent() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.OFF);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("old-session")
        );

        DiagnosticSessionTestResetResult reset =
                runtime.replaceOffSessionForTestsWithResult("fresh-session");
        DiagnosticSessionSnapshot disposed = reset.disposedSessionSnapshot();

        assertEquals(DiagnosticSessionTestResetResult.Status.SKIPPED_MODE_OFF, reset.status());
        assertEquals(null, reset.finalSnapshotEmission());
        assertEquals("old-session", disposed.diagnosticSessionId());
        assertEquals("fresh-session", runtime.snapshot().diagnosticSessionId());
        assertEquals(0, runtime.snapshot().admittedSlots());
    }

    @Test
    void preOffInvalidationRunsUnderTheExclusiveLeaseBeforeModePublication() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("off-order-session")
        );
        AtomicInteger callbackCount = new AtomicInteger();

        runtime.setBoundaryEnabled(false, () -> {
            assertTrue(mode.isBoundaryEnabled());
            assertEquals(0, runtime.snapshot().emissionPending());
            callbackCount.incrementAndGet();
        });
        runtime.setBoundaryEnabled(false, callbackCount::incrementAndGet);

        assertTrue(mode.isOff());
        assertEquals(1, callbackCount.get());
        assertEquals(0, runtime.snapshot().admittedSlots());
        assertEquals(0, runtime.snapshot().suppressedRequests());
    }

    @Test
    void cleanTeardownAdmitsAndSettlesExactlyOneFinalSnapshot() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("clean-session")
        );
        AtomicInteger emissionCount = new AtomicInteger();
        AtomicReference<DiagnosticSessionSnapshot> payload = new AtomicReference<>();

        DiagnosticDispatchResult first = runtime.emitCleanTeardownFinalSnapshot(snapshot -> {
            payload.set(snapshot);
            emissionCount.incrementAndGet();
        });
        DiagnosticDispatchResult repeated = runtime.emitCleanTeardownFinalSnapshot(
                ignored -> emissionCount.incrementAndGet()
        );

        assertTrue(first.admitted());
        assertFalse(repeated.admitted());
        assertEquals(1, emissionCount.get());
        assertTrue(payload.get().finalSnapshotAdmitted());
        assertEquals(1, payload.get().admittedSlots());
        assertEquals(1, payload.get().emissionPending());
        assertEquals(0, runtime.snapshot().emissionPending());
        assertEquals(1, runtime.snapshot().emissionCompleted());
        assertFalse(runtime.runIfEligible(() -> {
            throw new AssertionError("Clean teardown must close new diagnostic work.");
        }));
    }

    @Test
    void offCleanTeardownIsEventlessAndMutationFree() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.OFF);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("off-clean-session")
        );
        DiagnosticSessionSnapshot before = runtime.snapshot();

        DiagnosticDispatchResult result = runtime.emitCleanTeardownFinalSnapshot(ignored -> {
            throw new AssertionError("OFF teardown must not emit a final snapshot.");
        });

        assertFalse(result.admitted());
        assertEquals(before, runtime.snapshot());
    }

    @Test
    void readLeaseToModeWriteUpgradeFailsFastWithoutChangingMode() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("upgrade-session")
        );

        assertThrows(IllegalStateException.class, () -> runtime.runIfEligible(
                () -> runtime.setBoundaryEnabled(false)
        ));

        assertTrue(runtime.isEligible());
        assertTrue(mode.isBoundaryEnabled());
        assertEquals(0, runtime.snapshot().admittedSlots());
    }

    @Test
    void cleanTeardownKeepsEligibilityClosedEvenIfBoundaryEnableIsRequestedAgain() {
        DiagnosticModeController mode = new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("closed-session")
        );
        runtime.emitCleanTeardownFinalSnapshot(ignored -> {
        });

        runtime.setBoundaryEnabled(true);

        assertFalse(runtime.isEligible());
        AtomicInteger actionCalls = new AtomicInteger();
        assertFalse(runtime.runIfEligible(actionCalls::incrementAndGet));
        assertEquals(0, actionCalls.get());
        assertEquals(1, runtime.snapshot().admittedSlots());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

}
