package lavi.minecraft.diagnostics.mining.baritone.executor;

import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

//20260830_kpopmodder: Represent only the immutable result of executor-progress state evaluation.
final class BaritoneExecutorProgressEmission {
    private static final BaritoneExecutorProgressEmission SUPPRESSED =
            new BaritoneExecutorProgressEmission(false, null, "none", "none",
                    0, -1, -1, -1, -1, "unavailable", "unavailable",
                    "unavailable", "unavailable", "unavailable", "unavailable", "none");

    private final boolean emit;
    private final BaritoneExecutorProgressSnapshot headSnapshot;
    private final String reason;
    private final String changedFields;
    private final int localRepeatCount;
    private final long ticksSinceExecutorAdvance;
    private final long ticksSincePlayerMovement;
    private final long ticksSinceTargetDistanceImprovement;
    private final long ticksSinceAnyProgress;
    private final String executorPositionDeltaSinceHead;
    private final String executorPositionDeltaSinceLastReturn;
    private final String playerDisplacementSinceHead;
    private final String playerDisplacementSinceLastReturn;
    private final String targetDistanceDeltaSinceHead;
    private final String targetDistanceDeltaSinceLastReturn;
    private final String fingerprintCore;

    private BaritoneExecutorProgressEmission(boolean emit,
                                             BaritoneExecutorProgressSnapshot headSnapshot,
                                             String reason,
                                             String changedFields,
                                             int localRepeatCount,
                                             long ticksSinceExecutorAdvance,
                                             long ticksSincePlayerMovement,
                                             long ticksSinceTargetDistanceImprovement,
                                             long ticksSinceAnyProgress,
                                             String executorPositionDeltaSinceHead,
                                             String executorPositionDeltaSinceLastReturn,
                                             String playerDisplacementSinceHead,
                                             String playerDisplacementSinceLastReturn,
                                             String targetDistanceDeltaSinceHead,
                                             String targetDistanceDeltaSinceLastReturn,
                                             String fingerprintCore) {
        this.emit = emit;
        this.headSnapshot = headSnapshot;
        this.reason = reason;
        this.changedFields = changedFields;
        this.localRepeatCount = localRepeatCount;
        this.ticksSinceExecutorAdvance = ticksSinceExecutorAdvance;
        this.ticksSincePlayerMovement = ticksSincePlayerMovement;
        this.ticksSinceTargetDistanceImprovement = ticksSinceTargetDistanceImprovement;
        this.ticksSinceAnyProgress = ticksSinceAnyProgress;
        this.executorPositionDeltaSinceHead = executorPositionDeltaSinceHead;
        this.executorPositionDeltaSinceLastReturn = executorPositionDeltaSinceLastReturn;
        this.playerDisplacementSinceHead = playerDisplacementSinceHead;
        this.playerDisplacementSinceLastReturn = playerDisplacementSinceLastReturn;
        this.targetDistanceDeltaSinceHead = targetDistanceDeltaSinceHead;
        this.targetDistanceDeltaSinceLastReturn = targetDistanceDeltaSinceLastReturn;
        this.fingerprintCore = fingerprintCore;
    }

    static BaritoneExecutorProgressEmission suppressed() { return SUPPRESSED; }

    static BaritoneExecutorProgressEmission emit(BaritoneExecutorProgressSnapshot headSnapshot,
                                                  String reason,
                                                  String changedFields,
                                                  int localRepeatCount,
                                                  long ticksSinceExecutorAdvance,
                                                  long ticksSincePlayerMovement,
                                                  long ticksSinceTargetDistanceImprovement,
                                                  long ticksSinceAnyProgress,
                                                  String executorPositionDeltaSinceHead,
                                                  String executorPositionDeltaSinceLastReturn,
                                                  String playerDisplacementSinceHead,
                                                  String playerDisplacementSinceLastReturn,
                                                  String targetDistanceDeltaSinceHead,
                                                  String targetDistanceDeltaSinceLastReturn,
                                                  String fingerprintCore) {
        return new BaritoneExecutorProgressEmission(true, headSnapshot, reason, changedFields, localRepeatCount,
                ticksSinceExecutorAdvance, ticksSincePlayerMovement, ticksSinceTargetDistanceImprovement,
                ticksSinceAnyProgress, executorPositionDeltaSinceHead, executorPositionDeltaSinceLastReturn,
                playerDisplacementSinceHead, playerDisplacementSinceLastReturn, targetDistanceDeltaSinceHead,
                targetDistanceDeltaSinceLastReturn, fingerprintCore);
    }

    boolean emit() { return emit; }
    BaritoneExecutorProgressSnapshot headSnapshot() { return headSnapshot; }
    String reason() { return reason; }
    String changedFields() { return changedFields; }
    int localRepeatCount() { return localRepeatCount; }
    long ticksSinceExecutorAdvance() { return ticksSinceExecutorAdvance; }
    long ticksSincePlayerMovement() { return ticksSincePlayerMovement; }
    long ticksSinceTargetDistanceImprovement() { return ticksSinceTargetDistanceImprovement; }
    long ticksSinceAnyProgress() { return ticksSinceAnyProgress; }
    String executorPositionDeltaSinceHead() { return executorPositionDeltaSinceHead; }
    String executorPositionDeltaSinceLastReturn() { return executorPositionDeltaSinceLastReturn; }
    String playerDisplacementSinceHead() { return playerDisplacementSinceHead; }
    String playerDisplacementSinceLastReturn() { return playerDisplacementSinceLastReturn; }
    String targetDistanceDeltaSinceHead() { return targetDistanceDeltaSinceHead; }
    String targetDistanceDeltaSinceLastReturn() { return targetDistanceDeltaSinceLastReturn; }

    String fingerprint(String eventName) {
        return MiningDiagnosticEmitter.joinFingerprint(eventName, reason, fingerprintCore);
    }
}
