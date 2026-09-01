package lavi.minecraft.diagnostics.session.emission;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionRequest;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticFamilySnapshot;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticEmissionCoordinatorTest {
    @Test
    void formatterFailureConsumesTheAdmittedSlotWithoutRefundOrRetry() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("formatter-failure-session");
        DiagnosticAdmissionDecision admission = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.EXCEPTION_COVERAGE));
        AtomicInteger formatterCalls = new AtomicInteger();
        AtomicInteger sinkCalls = new AtomicInteger();
        DiagnosticEmissionCoordinator coordinator = new DiagnosticEmissionCoordinator(authority);

        DiagnosticEmissionOutcome first = coordinator.emit(
                admission.token(),
                "record",
                ignored -> {
                    formatterCalls.incrementAndGet();
                    throw new IllegalStateException("formatter failed");
                },
                ignored -> sinkCalls.incrementAndGet()
        );

        assertEquals(DiagnosticEmissionOutcome.Status.EMISSION_FAILED_AFTER_ADMISSION, first.status());
        assertEquals(DiagnosticEmissionOutcome.FailureStage.FORMATTER, first.failureStage());
        assertEquals(1, formatterCalls.get());
        assertEquals(0, sinkCalls.get());
        assertEquals(1, authority.snapshot().admittedSlots());
        assertEquals(0, authority.snapshot().emissionPending());
        assertEquals(1, authority.snapshot().emissionFailedAfterAdmission());

        DiagnosticEmissionOutcome repeated = coordinator.emit(
                admission.token(),
                "record",
                ignored -> {
                    formatterCalls.incrementAndGet();
                    return "encoded";
                },
                ignored -> sinkCalls.incrementAndGet()
        );
        assertEquals(DiagnosticEmissionOutcome.Status.TOKEN_UNAVAILABLE, repeated.status());
        assertEquals(1, formatterCalls.get());
        assertEquals(0, sinkCalls.get());
        assertEquals(1, authority.snapshot().admittedSlots());
    }

    @Test
    void sinkFailureSettlesFailedAfterOnePhysicalCall() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("sink-failure-session");
        DiagnosticAdmissionDecision admission = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.NON_STORE_TERMINAL));
        AtomicInteger sinkCalls = new AtomicInteger();

        DiagnosticEmissionOutcome outcome = new DiagnosticEmissionCoordinator(authority).emit(
                admission.token(),
                "record",
                value -> value,
                ignored -> {
                    sinkCalls.incrementAndGet();
                    throw new IllegalStateException("sink failed");
                }
        );

        assertEquals(DiagnosticEmissionOutcome.Status.EMISSION_FAILED_AFTER_ADMISSION, outcome.status());
        assertEquals(DiagnosticEmissionOutcome.FailureStage.SINK, outcome.failureStage());
        assertEquals(1, sinkCalls.get());
        DiagnosticFamilySnapshot family = authority.snapshot().family(DiagnosticEventFamily.NON_STORE_TERMINAL);
        assertEquals(1, family.admittedRequests());
        assertEquals(0, family.emissionPending());
        assertEquals(0, family.emissionCompleted());
        assertEquals(1, family.emissionFailedAfterAdmission());
    }

    @Test
    void successfulSinkReturnSettlesCompletedButDoesNotClaimDurableDelivery() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("successful-emission-session");
        DiagnosticAdmissionDecision admission = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.AGGREGATE_CHECKPOINT));
        AtomicInteger sinkCalls = new AtomicInteger();

        DiagnosticEmissionOutcome outcome = new DiagnosticEmissionCoordinator(authority).emit(
                admission.token(),
                "record",
                value -> "encoded=" + value,
                ignored -> sinkCalls.incrementAndGet()
        );

        assertTrue(outcome.completed());
        assertEquals(DiagnosticEmissionOutcome.Status.EMISSION_CALLS_RETURNED, outcome.status());
        assertEquals(1, sinkCalls.get());
        assertEquals(0, authority.snapshot().emissionPending());
        assertEquals(1, authority.snapshot().emissionCompleted());
        assertEquals(0, authority.snapshot().emissionFailedAfterAdmission());
    }

    @Test
    void anAlreadyStartedTokenCannotFormatOrEmitTwice() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("lease-session");
        DiagnosticAdmissionDecision admission = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.SUPPRESSION_CONTROL));
        assertTrue(authority.beginEmission(admission.token()).started());
        AtomicInteger formatterCalls = new AtomicInteger();

        DiagnosticEmissionOutcome duplicate = new DiagnosticEmissionCoordinator(authority).emit(
                admission.token(),
                "record",
                value -> {
                    formatterCalls.incrementAndGet();
                    return value;
                },
                ignored -> {
                }
        );

        assertEquals(DiagnosticEmissionOutcome.Status.TOKEN_UNAVAILABLE, duplicate.status());
        assertEquals(0, formatterCalls.get());
        assertEquals(1, authority.snapshot().emissionPending());
        assertEquals(1, authority.snapshot().emissionInProgress());
        assertFalse(duplicate.completed());
    }

    @Test
    void canonicalCapFormatterFailureDoesNotReclaimOrRecursivelyAdmitTheCapSlot() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("cap-failure-session");
        for (int index = 0; index < DiagnosticSessionLimits.ORDINARY_CEILING; index++) {
            assertTrue(authority.admit(
                    DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        }
        DiagnosticAdmissionDecision rejected = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL));
        AtomicInteger formatterCalls = new AtomicInteger();

        DiagnosticEmissionOutcome failed = new DiagnosticEmissionCoordinator(authority).emit(
                rejected.canonicalCapToken(),
                "cap",
                ignored -> {
                    formatterCalls.incrementAndGet();
                    throw new IllegalStateException("cap formatter failed");
                },
                ignored -> {
                }
        );
        assertEquals(DiagnosticEmissionOutcome.Status.EMISSION_FAILED_AFTER_ADMISSION, failed.status());
        assertEquals(1, formatterCalls.get());

        for (int index = 0; index < 100; index++) {
            assertFalse(authority.admit(
                    DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL))
                    .canonicalCapEventClaimedByThisDecision());
        }
        DiagnosticFamilySnapshot cap = authority.snapshot().family(DiagnosticEventFamily.CANONICAL_CAP);
        assertEquals(1, cap.admittedRequests());
        assertEquals(0, cap.emissionPending());
        assertEquals(1, cap.emissionFailedAfterAdmission());
        assertEquals(4_937, authority.snapshot().admittedSlots());
    }
}
