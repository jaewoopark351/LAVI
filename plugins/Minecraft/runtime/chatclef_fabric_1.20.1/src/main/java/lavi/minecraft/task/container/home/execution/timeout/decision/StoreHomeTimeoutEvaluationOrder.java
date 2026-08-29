package lavi.minecraft.task.container.home.execution.timeout.decision;

//20260828_kpopmodder: Define when candidate observation may precede an operation no-progress decision.
public final class StoreHomeTimeoutEvaluationOrder {
    private StoreHomeTimeoutEvaluationOrder() {
    }

    public static StoreHomeOperationNoProgressCheckpoint operationNoProgressCheckpoint(
            boolean candidateAttemptActive,
            boolean containerSessionActive,
            boolean transferPending) {
        boolean narrowProgressProofAllowed = candidateAttemptActive
                && !containerSessionActive
                && !transferPending;
        return narrowProgressProofAllowed
                ? StoreHomeOperationNoProgressCheckpoint
                        .OBSERVE_POSITION_AND_TRY_EXACT_ACTIVATION_ONLY
                : StoreHomeOperationNoProgressCheckpoint.RESOLVE_BEFORE_BEHAVIOR;
    }
}
