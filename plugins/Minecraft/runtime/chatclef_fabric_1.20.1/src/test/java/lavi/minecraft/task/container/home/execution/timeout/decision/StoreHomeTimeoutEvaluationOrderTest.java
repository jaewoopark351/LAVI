package lavi.minecraft.task.container.home.execution.timeout.decision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260828_kpopmodder: Fix the operation timeout checkpoints around candidate observation.
class StoreHomeTimeoutEvaluationOrderTest {
    @Test
    void activeCandidateMayProveMovementOrExactActivationAtNoProgressBoundary() {
        assertEquals(
                StoreHomeOperationNoProgressCheckpoint
                        .OBSERVE_POSITION_AND_TRY_EXACT_ACTIVATION_ONLY,
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        true, false, false
                )
        );
    }

    @Test
    void noCandidateCannotHideTimeoutBySelectingAnotherCandidate() {
        assertEquals(
                StoreHomeOperationNoProgressCheckpoint.RESOLVE_BEFORE_BEHAVIOR,
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        false, false, false
                )
        );
    }

    @Test
    void activeContainerSessionCannotIssueANewClickAtTimeoutBoundary() {
        assertEquals(
                StoreHomeOperationNoProgressCheckpoint.RESOLVE_BEFORE_BEHAVIOR,
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        true, true, false
                )
        );
    }

    @Test
    void pendingTransferAlwaysResolvesBeforeFurtherBehavior() {
        assertEquals(
                StoreHomeOperationNoProgressCheckpoint.RESOLVE_BEFORE_BEHAVIOR,
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        true, true, true
                )
        );
    }
}
