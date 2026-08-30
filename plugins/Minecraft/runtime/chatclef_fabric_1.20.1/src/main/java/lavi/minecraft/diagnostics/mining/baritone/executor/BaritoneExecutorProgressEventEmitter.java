package lavi.minecraft.diagnostics.mining.baritone.executor;

import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

//20260830_kpopmodder: Own only the bounded executor-progress event contract and permit-gated detail output.
final class BaritoneExecutorProgressEventEmitter {
    private static final String EVENT_NAME = "BARITONE_EXECUTOR_PROGRESS_SNAPSHOT";
    private static final String EVENT_REASON = "baritone_executor_progress_snapshot";
    private static final String OWNER = "baritone_pathing_behavior_executor_observer";

    private BaritoneExecutorProgressEventEmitter() {
    }

    static void emit(BaritoneExecutorProgressSnapshot snapshot,
                     BaritoneExecutorProgressState.Emission emission) {
        String bucket = "baritone_executor_progress|" + snapshot.pathingBehaviorIdentity();
        String fingerprint = emission.fingerprint(EVENT_NAME);
        MiningDiagnosticEmitter.emitLazyWithFallback(
                EVENT_NAME,
                EVENT_REASON,
                null,
                bucket,
                fingerprint,
                bucket,
                "BARITONE_BUCKET_OR_EXECUTOR_FALLBACK",
                () -> MiningDiagnosticEmitter.merge(new Object[]{
                                "owner", OWNER,
                                "trigger", "pathing_behavior_tick_path_return",
                                "correlation", bucket,
                                "progressReason", emission.reason(),
                                "changedFields", emission.changedFields(),
                                "localRepeatCount", emission.localRepeatCount(),
                                "ticksSinceExecutorAdvance", emission.ticksSinceExecutorAdvance(),
                                "ticksSincePlayerMovement", emission.ticksSincePlayerMovement(),
                                "ticksSinceTargetDistanceImprovement", emission.ticksSinceTargetDistanceImprovement(),
                                "ticksSinceAnyProgress", emission.ticksSinceAnyProgress(),
                                "executorPositionDeltaSinceHead", emission.executorPositionDeltaSinceHead(),
                                "executorPositionDeltaSinceLastReturn", emission.executorPositionDeltaSinceLastReturn(),
                                "playerDisplacementSinceHead", emission.playerDisplacementSinceHead(),
                                "playerDisplacementSinceLastReturn", emission.playerDisplacementSinceLastReturn(),
                                "targetDistanceDeltaSinceHead", emission.targetDistanceDeltaSinceHead(),
                                "targetDistanceDeltaSinceLastReturn", emission.targetDistanceDeltaSinceLastReturn()
                        },
                        BaritoneExecutorProgressSnapshot.fieldsOrUnavailable(emission.headSnapshot(), "Before"),
                        snapshot.fields("After"))
        );
    }
}
