package lavi.minecraft.task.container.home.execution.timeout.decision;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Fix pending precedence and all four stable timeout reason values.
class StoreHomeTimeoutResolverTest {
    @Test
    void everyMatchedTimeoutWithPendingBecomesTransferUnconfirmed() {
        for (StoreHomeTimeoutReason reason : StoreHomeTimeoutReason.values()) {
            StoreHomeTimeoutDecision decision = resolveWithPending(reason);

            assertEquals(
                    StoreHomeTimeoutAction.FINISH_TRANSFER_UNCONFIRMED,
                    decision.action()
            );
            assertEquals(
                    reason.stableReason() + "_with_pending_transfer",
                    decision.terminalReason()
            );
        }
    }

    @Test
    void noPendingKeepsOperationAndCandidateActionsSeparate() {
        StoreHomeTimeoutDecision operation =
                StoreHomeTimeoutResolver.resolveOperation(
                        false,
                        Optional.of(StoreHomeTimeoutReason.OPERATION_NO_PROGRESS)
                ).orElseThrow();
        StoreHomeTimeoutDecision candidate =
                StoreHomeTimeoutResolver.resolveCandidate(
                        false,
                        Optional.of(
                                StoreHomeTimeoutReason
                                        .CANDIDATE_NAVIGATION_NO_PROGRESS
                        )
                ).orElseThrow();

        assertEquals(StoreHomeTimeoutAction.FINISH_OPERATION, operation.action());
        assertEquals(StoreHomeTimeoutAction.REJECT_CANDIDATE, candidate.action());
    }

    @Test
    void emergencyHardCapCanNeverBecomeCandidateRejection() {
        StoreHomeTimeoutDecision decision =
                StoreHomeTimeoutResolver.resolveOperation(
                        false,
                        Optional.of(
                                StoreHomeTimeoutReason
                                        .OPERATION_EMERGENCY_HARD_CAP
                        )
                ).orElseThrow();

        assertEquals(StoreHomeTimeoutAction.FINISH_OPERATION, decision.action());
        assertEquals("operation_emergency_hard_cap", decision.terminalReason());
    }

    @Test
    void noMatchedTimeoutProducesNoDecision() {
        assertTrue(StoreHomeTimeoutResolver.resolveOperation(
                true, Optional.empty()
        ).isEmpty());
        assertTrue(StoreHomeTimeoutResolver.resolveCandidate(
                true, Optional.empty()
        ).isEmpty());
    }

    @Test
    void scopeMixupsFailClosed() {
        assertThrows(IllegalArgumentException.class, () ->
                StoreHomeTimeoutResolver.resolveCandidate(
                        false,
                        Optional.of(StoreHomeTimeoutReason.OPERATION_NO_PROGRESS)
                ));
        assertThrows(IllegalArgumentException.class, () ->
                StoreHomeTimeoutResolver.resolveOperation(
                        false,
                        Optional.of(
                                StoreHomeTimeoutReason
                                        .CANDIDATE_LOCAL_INTERACTION_TIMEOUT
                        )
                ));
    }

    @Test
    void stableReasonCodesRemainDistinct() {
        assertEquals("candidate_navigation_no_progress",
                StoreHomeTimeoutReason.CANDIDATE_NAVIGATION_NO_PROGRESS
                        .stableReason());
        assertEquals("candidate_local_interaction_timeout",
                StoreHomeTimeoutReason.CANDIDATE_LOCAL_INTERACTION_TIMEOUT
                        .stableReason());
        assertEquals("operation_no_progress",
                StoreHomeTimeoutReason.OPERATION_NO_PROGRESS.stableReason());
        assertEquals("operation_emergency_hard_cap",
                StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP
                        .stableReason());
    }

    private static StoreHomeTimeoutDecision resolveWithPending(
            StoreHomeTimeoutReason reason) {
        if (reason.candidateScoped()) {
            return StoreHomeTimeoutResolver.resolveCandidate(
                    true, Optional.of(reason)
            ).orElseThrow();
        }
        return StoreHomeTimeoutResolver.resolveOperation(
                true, Optional.of(reason)
        ).orElseThrow();
    }
}
