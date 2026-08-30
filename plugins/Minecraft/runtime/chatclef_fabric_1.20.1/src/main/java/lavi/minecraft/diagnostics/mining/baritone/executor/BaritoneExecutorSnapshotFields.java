package lavi.minecraft.diagnostics.mining.baritone.executor;

import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

//20260830_kpopmodder: Project already captured executor values into the existing event-field contract.
final class BaritoneExecutorSnapshotFields {
    private BaritoneExecutorSnapshotFields() {
    }

    static Object[] project(BaritoneExecutorProgressObservation observation,
                            BaritoneExecutorDetailSnapshot detail,
                            String suffix) {
        return MiningDiagnosticEmitter.merge(new Object[]{
                        "snapshotPresent" + suffix, true,
                        "snapshotPhase" + suffix, observation.phase(),
                        "detailCapturePolicy" + suffix, "PERMIT_GATED_FROM_RETAINED_REFERENCES",
                        "gameTick" + suffix, observation.tick(),
                        "threadName" + suffix, observation.threadName(),
                        "pathingBehaviorIdentity" + suffix, observation.pathingBehaviorIdentity(),
                        "pathingBehaviorType" + suffix, observation.pathingBehaviorType(),
                        "baritonePathing" + suffix, observation.pathing(),
                        "safeToCancel" + suffix, detail.safeToCancel,
                        "estimatedTicksToGoal" + suffix, detail.estimatedTicksToGoal,
                        "expectedSegmentStart" + suffix, detail.expectedSegmentStart,
                        "cancelRequested" + suffix, observation.cancelRequested(),
                        "calcFailedLastTick" + suffix, observation.calcFailedLastTick(),
                        "goalIdentity" + suffix, observation.goalIdentity(),
                        "goalType" + suffix, detail.goalType,
                        "goalSummary" + suffix, detail.goalSummary,
                        "inProgressIdentity" + suffix, observation.inProgressIdentity(),
                        "inProgressType" + suffix, detail.inProgressType,
                        "inProgressSummary" + suffix, detail.inProgressSummary
                },
                executorFields(detail.currentExecutor, "current", suffix),
                executorFields(detail.nextExecutor, "next", suffix),
                playerFields(detail.player, suffix),
                detail.target.fields(suffix));
    }

    private static Object[] executorFields(BaritoneExecutorStateSnapshot executor,
                                           String role,
                                           String suffix) {
        String prefix = role + "Executor";
        return new Object[]{
                prefix + "Present" + suffix, executor.present,
                prefix + "Identity" + suffix, executor.identity,
                prefix + "Type" + suffix, executor.type,
                prefix + "Position" + suffix, executor.positionText,
                prefix + "Failed" + suffix, executor.failed,
                prefix + "Finished" + suffix, executor.finished,
                prefix + "PathIdentity" + suffix, executor.pathIdentity,
                prefix + "PathType" + suffix, executor.pathType,
                prefix + "PathLength" + suffix, executor.pathLength,
                prefix + "PathMovementCount" + suffix, executor.pathMovementCount,
                prefix + "PathPositionCount" + suffix, executor.pathPositionCount,
                prefix + "PathSrc" + suffix, executor.pathSrc,
                prefix + "PathDest" + suffix, executor.pathDest,
                prefix + "PathGoal" + suffix, executor.pathGoal
        };
    }

    private static Object[] playerFields(BaritonePlayerProgressSnapshot player, String suffix) {
        return new Object[]{
                "playerPresent" + suffix, player.present,
                "playerPosition" + suffix, player.position,
                "playerBlockPosition" + suffix, player.blockPosition,
                "playerVelocity" + suffix, player.velocity,
                "playerSpeedSq" + suffix, player.speedSq,
                "dimension" + suffix, player.dimension,
                "playerPoseState" + suffix, player.poseState
        };
    }
}
