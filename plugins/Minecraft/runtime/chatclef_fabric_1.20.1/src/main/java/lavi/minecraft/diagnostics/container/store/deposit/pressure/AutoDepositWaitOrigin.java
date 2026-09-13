package lavi.minecraft.diagnostics.container.store.deposit.pressure;

//20260913_kpopmodder: Keep the original wait cause across root changes without retaining live Tasks.
public record AutoDepositWaitOrigin(String state, String reason, String taskIdentity,
                                    String commandRequestId, long enteredTick,
                                    long evaluationSequence, long trustedRevision) {
    public Object[] fields(long currentTick) {
        return new Object[]{
                "waitOriginState", state,
                "waitOriginReason", reason,
                "waitOriginTask", taskIdentity,
                "waitOriginRequestId", commandRequestId,
                "waitEnteredTick", enteredTick,
                "waitElapsedTicks", Math.max(0L, currentTick - enteredTick),
                "waitOriginEvaluationSequence", evaluationSequence,
                "waitOriginTrustedRevision", trustedRevision
        };
    }
}
