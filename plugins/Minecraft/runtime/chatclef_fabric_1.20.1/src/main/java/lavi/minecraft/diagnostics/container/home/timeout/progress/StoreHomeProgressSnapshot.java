package lavi.minecraft.diagnostics.container.home.timeout.progress;

import net.minecraft.util.math.BlockPos;

//20260828_kpopmodder: Carry one read-only STORE_HOME movement, pathing, and container observation.
public record StoreHomeProgressSnapshot(
        BlockPos playerBlockPosition,
        String playerPosition,
        Long currentDistanceSquared3d,
        String baritonePathingActive,
        String baritoneCalculationState,
        String baritonePathPresent,
        String customGoalActive,
        String normalizedGoalType,
        String normalizedGoalTarget,
        String goalMatchesCandidatePosition,
        String screenClass,
        String handlerClass,
        Object syncId,
        String exactBindingMatched,
        String containerSessionState,
        Object containerSessionOrdinal,
        Object planRevision,
        String sessionPendingTransfer,
        String executorPendingTransfer,
        int pathingObservationUnavailableCount,
        String captureStatus,
        String captureErrors) {

    public Object distanceValue() {
        return currentDistanceSquared3d == null
                ? "unavailable"
                : currentDistanceSquared3d;
    }

    public String pathingStateKey() {
        return join(
                baritonePathingActive,
                baritoneCalculationState,
                baritonePathPresent
        );
    }

    public String goalStateKey() {
        return join(
                customGoalActive,
                normalizedGoalType,
                normalizedGoalTarget,
                goalMatchesCandidatePosition
        );
    }

    public String bindingStateKey() {
        return join(
                exactBindingMatched,
                screenClass,
                handlerClass,
                String.valueOf(syncId)
        );
    }

    public String containerStateKey() {
        return join(
                containerSessionState,
                String.valueOf(containerSessionOrdinal),
                String.valueOf(planRevision),
                sessionPendingTransfer,
                executorPendingTransfer
        );
    }

    private static String join(String... values) {
        return String.join("|", values);
    }
}
