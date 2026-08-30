package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.cancel.PreCancelBaritoneState;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Project passive DestroyBlockTask lifetime fields after emission admission.
final class DestroyBlockLifetimeEventFields {
    private DestroyBlockLifetimeEventFields() {
    }

    static Object[] capture(String phase,
                            AltoClef mod,
                            DestroyBlockTask task,
                            BlockPos target,
                            Task interruptTask,
                            String forceCancelSource,
                            DestroyBlockDiagnosticState.State state,
                            PreCancelBaritoneState cancelBefore,
                            long tick) {
        DestroyBlockDiagnosticState.updateObservedProgress(mod, target, state);
        BaritonePathDiagnosticSnapshot cancelAfter = BaritonePathDiagnosticSnapshot.capture(
                mod, target, null, "unavailable");
        int cobblestoneAtStop = "STOP".equals(phase)
                ? DestroyBlockDiagnosticFields.cobblestoneCount(mod)
                : -1;
        return MiningDiagnosticEmitter.merge(new Object[]{
                "owner", "destroy_block_lifetime_observer",
                "trigger", "START".equals(phase) ? "destroy_on_start" : "destroy_on_stop",
                "destroyTaskRunId", state.runId,
                "destroyTaskInstanceId", MiningDiagnosticEmitter.instanceId(task),
                "phase", phase,
                "targetPosition", ChatClefDiagnostics.blockPos(target),
                "targetBlockState", DestroyBlockDiagnosticFields.targetBlockState(mod, target),
                "blockStillExists", DestroyBlockDiagnosticFields.blockStillExists(mod, target),
                "startedAtTick", state.startedAtTick,
                "stoppedAtTick", "STOP".equals(phase) ? tick : "unavailable",
                "lifetimeTicks", "STOP".equals(phase) ? Math.max(0, tick - state.startedAtTick) : 0,
                "parentContainerDecisionSequence", "unavailable_without_parent_link",
                "parentEffectiveBranch", "unavailable_without_parent_link",
                "interruptTaskClass", MiningDiagnosticEmitter.taskClass(interruptTask),
                "interruptTaskSemanticKey", ChatClefDiagnostics.taskSummary(interruptTask),
                "forceCancelSource", forceCancelSource,
                "pathSuccessObserved", state.pathSuccessObserved,
                "pathingStartedObserved", state.pathingStartedObserved,
                "reachEverPresent", state.reachEverPresent,
                "breakEverStarted", state.breakEverStarted,
                "maximumBreakingProgress", state.maximumBreakingProgress,
                "blockBecameAir", state.blockBecameAir,
                "inventoryCobblestoneAtStart", state.inventoryCobblestoneAtStart,
                "inventoryCobblestoneAtStop", "STOP".equals(phase) ? cobblestoneAtStop : "unavailable"
        }, cancelBefore == null ? new Object[0] : cancelBefore.fields("Before"),
                cancelAfter.fields("After"));
    }
}
