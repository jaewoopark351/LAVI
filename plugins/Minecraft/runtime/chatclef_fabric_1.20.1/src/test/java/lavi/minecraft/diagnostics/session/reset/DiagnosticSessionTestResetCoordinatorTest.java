package lavi.minecraft.diagnostics.session.reset;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionRequest;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionCoordinator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSessionTestResetCoordinatorTest {
    @Test
    void offResetSkipsFinalSnapshotWithoutMutatingTheDisposedSession() {
        DiagnosticSessionAdmissionAuthority current =
                new DiagnosticSessionAdmissionAuthority("off-old-session");
        complete(current, current.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL)));
        DiagnosticSessionSnapshot before = current.snapshot();
        AtomicInteger formatterCalls = new AtomicInteger();
        AtomicInteger sinkCalls = new AtomicInteger();

        DiagnosticSessionTestResetResult result = new DiagnosticSessionTestResetCoordinator().reset(
                current,
                "off-fresh-session",
                false,
                snapshot -> {
                    formatterCalls.incrementAndGet();
                    return snapshot.toString();
                },
                ignored -> sinkCalls.incrementAndGet()
        );

        assertEquals(DiagnosticSessionTestResetResult.Status.SKIPPED_MODE_OFF, result.status());
        assertEquals(before, current.snapshot());
        assertEquals(before, result.disposedSessionSnapshot());
        assertEquals(0, formatterCalls.get());
        assertEquals(0, sinkCalls.get());
        assertEquals("off-fresh-session", result.freshSession().snapshot().diagnosticSessionId());
        assertEquals(0, result.freshSession().snapshot().admittedSlots());
        assertEquals(0, result.freshSession().snapshot().suppressedRequests());
    }

    @Test
    void hardCapResetCloses4998Cap4999Snapshot5000BeforeFreshSession() {
        DiagnosticSessionAdmissionAuthority current =
                new DiagnosticSessionAdmissionAuthority("hard-old-session");
        fillNormalCapacityAndComplete(current);
        assertEquals(4_998, current.snapshot().admittedSlots());
        assertEquals(0, current.snapshot().emissionPending());

        DiagnosticAdmissionDecision hardRejected =
                current.admit(eligible(DiagnosticEventFamily.EXCEPTION_COVERAGE));
        assertNotNull(hardRejected.canonicalCapToken());
        completeToken(current, hardRejected.canonicalCapToken());
        assertEquals(4_999, current.snapshot().admittedSlots());
        assertEquals(0, current.snapshot().emissionPending());

        AtomicReference<DiagnosticSessionSnapshot> formattedSnapshot = new AtomicReference<>();
        AtomicInteger sinkCalls = new AtomicInteger();
        DiagnosticSessionTestResetResult result = new DiagnosticSessionTestResetCoordinator().reset(
                current,
                "hard-fresh-session",
                true,
                snapshot -> {
                    formattedSnapshot.set(snapshot);
                    return "session=" + snapshot.diagnosticSessionId();
                },
                ignored -> sinkCalls.incrementAndGet()
        );

        assertEquals(DiagnosticSessionTestResetResult.Status.RESET_COMPLETED, result.status());
        assertEquals(1, sinkCalls.get());
        assertNotNull(formattedSnapshot.get());
        assertEquals(5_000, formattedSnapshot.get().admittedSlots());
        assertEquals(1, formattedSnapshot.get().emissionPending());
        assertEquals(5_000, result.disposedSessionSnapshot().admittedSlots());
        assertEquals(0, result.disposedSessionSnapshot().emissionPending());
        assertEquals(result.disposedSessionSnapshot().admittedRequests(),
                result.disposedSessionSnapshot().emissionCompleted());
        assertEquals("hard-fresh-session", result.freshSession().snapshot().diagnosticSessionId());
        assertEquals(0, result.freshSession().snapshot().admittedSlots());
    }

    @Test
    void eligibleResetSettlesAFailedFinalSnapshotBeforeReplacement() {
        DiagnosticSessionAdmissionAuthority current =
                new DiagnosticSessionAdmissionAuthority("failed-old-session");

        DiagnosticSessionTestResetResult result = new DiagnosticSessionTestResetCoordinator().reset(
                current,
                "failed-fresh-session",
                true,
                ignored -> {
                    throw new IllegalStateException("snapshot formatter failed");
                },
                ignored -> {
                }
        );

        assertEquals(DiagnosticSessionTestResetResult.Status.RESET_COMPLETED_WITH_FAILED_SNAPSHOT,
                result.status());
        assertEquals(1, result.disposedSessionSnapshot().admittedSlots());
        assertEquals(0, result.disposedSessionSnapshot().emissionPending());
        assertEquals(1, result.disposedSessionSnapshot().emissionFailedAfterAdmission());
        assertEquals(0, result.freshSession().snapshot().admittedSlots());
    }

    @Test
    void resetFailsClosedWhileAnEarlierAdmissionRemainsPending() {
        DiagnosticSessionAdmissionAuthority current =
                new DiagnosticSessionAdmissionAuthority("pending-old-session");
        assertTrue(current.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        DiagnosticSessionSnapshot before = current.snapshot();

        assertThrows(IllegalStateException.class, () -> new DiagnosticSessionTestResetCoordinator().reset(
                current,
                "pending-fresh-session",
                true,
                Object::toString,
                ignored -> {
                }
        ));
        assertEquals(before, current.snapshot());
    }

    private static DiagnosticAdmissionRequest eligible(DiagnosticEventFamily family) {
        return DiagnosticAdmissionRequest.eligible(family);
    }

    private static void fillNormalCapacityAndComplete(DiagnosticSessionAdmissionAuthority authority) {
        for (int index = 0; index < DiagnosticSessionLimits.ORDINARY_CEILING; index++) {
            complete(authority, authority.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL)));
        }
        admitAndComplete(authority, DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL, 4);
        admitAndComplete(authority, DiagnosticEventFamily.ROUTINE_STORE_TERMINAL, 2);
        admitAndComplete(authority, DiagnosticEventFamily.EXCEPTION_COVERAGE, 16);
        admitAndComplete(authority, DiagnosticEventFamily.AGGREGATE_CHECKPOINT, 8);
        admitAndComplete(authority, DiagnosticEventFamily.NON_STORE_TERMINAL, 8);
        admitAndComplete(authority, DiagnosticEventFamily.SUPPRESSION_CONTROL, 6);
    }

    private static void admitAndComplete(DiagnosticSessionAdmissionAuthority authority,
                                         DiagnosticEventFamily family,
                                         int count) {
        for (int index = 0; index < count; index++) {
            complete(authority, authority.admit(eligible(family)));
        }
    }

    private static void complete(DiagnosticSessionAdmissionAuthority authority,
                                 DiagnosticAdmissionDecision admission) {
        assertTrue(admission.admitted());
        completeToken(authority, admission.token());
    }

    private static void completeToken(DiagnosticSessionAdmissionAuthority authority,
                                      lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionToken token) {
        assertTrue(new DiagnosticEmissionCoordinator(authority).emit(
                token,
                "record",
                value -> value,
                ignored -> {
                }
        ).completed());
    }
}
