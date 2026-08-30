package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.finish.DestroyFinishEvaluationOrigin;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Project the existing DestroyBlockTask finish result without re-evaluating behavior.
final class DestroyFinishEvaluationDiagnostics {
    private DestroyFinishEvaluationDiagnostics() {
    }

    static void log(AltoClef mod,
                    DestroyBlockTask task,
                    BlockPos target,
                    BlockState observedBlockState,
                    boolean observedIsAir) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        Task currentTask = ChatClefDiagnostics.currentTaskForDiagnostics();
        boolean ownsCurrentContext = currentTask == task;
        // A parent Task being current proves only that it called this predicate. It does not prove
        // that the call was diagnostic. No diagnostic probe remains in this slice, so only an
        // explicitly scoped probe may set this evidence in a later change.
        boolean exactDiagnosticProbeEvidence = false;
        DestroyFinishEvaluationOrigin origin = DestroyFinishEvaluationOrigin.classify(
                ownsCurrentContext,
                exactDiagnosticProbeEvidence
        );
        Object destroyRunId = DestroyBlockDiagnosticState.runIdIfPresent(task);
        String targetText = ChatClefDiagnostics.blockPos(target);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "DESTROY_FINISH_EVALUATION",
                targetText,
                String.valueOf(destroyRunId),
                origin.name(),
                observedBlockState == null ? "unavailable" : String.valueOf(observedBlockState.getBlock()),
                Boolean.toString(observedIsAir)
        );
        MiningDiagnosticEmitter.emitLazy(
                "DESTROY_FINISH_EVALUATION",
                "destroy_finish_evaluation",
                task,
                "destroy_finish|" + targetText + "|run=" + destroyRunId + "|origin=" + origin.name(),
                fingerprint,
                () -> new Object[]{
                        "owner", "destroy_finish_evaluation_observer",
                        "trigger", "existing_is_finished_result",
                        "modAvailable", mod != null,
                        "destroyTaskRunId", destroyRunId,
                        "targetPosition", targetText,
                        "targetBlockState", observedBlockState == null ? "unavailable" : observedBlockState,
                        "blockStateCaptureSource", "EXISTING_DESTROY_IS_FINISHED_LOCAL",
                        "isAir", observedIsAir,
                        "isAirCaptureSource", "EXISTING_BLOCK_STATE_RESULT",
                        "finishEvaluationOrigin", origin.name(),
                        "originEvidence", originEvidence(origin),
                        "currentDiagnosticTaskClass", MiningDiagnosticEmitter.taskClass(currentTask),
                        "currentDiagnosticTaskInstanceId", MiningDiagnosticEmitter.instanceId(currentTask),
                        "evaluatedTaskActive", MiningDiagnosticEmitter.safeTaskActive(task),
                        "evaluatedTaskStopped", MiningDiagnosticEmitter.safeTaskStopped(task),
                        "candidateRunAllocationPerformed", false,
                        "worldReevaluationPerformed", false,
                        "callerCoverage", origin == DestroyFinishEvaluationOrigin.UNKNOWN_CALLER
                                ? "UNAVAILABLE_OUTSIDE_TASK_CONTEXT"
                                : "DIRECT_TASK_CONTEXT"
                }
        );
    }

    private static String originEvidence(DestroyFinishEvaluationOrigin origin) {
        return switch (origin) {
            case ACTIVE_TASK_LIFECYCLE -> "CURRENT_DIAGNOSTIC_TASK_EQUALS_EVALUATED_TASK";
            case PARENT_DIAGNOSTIC_PROBE -> "EXPLICIT_DIAGNOSTIC_PROBE_SCOPE";
            case UNKNOWN_CALLER -> "NO_DIRECT_CALLER_EVIDENCE";
        };
    }
}
