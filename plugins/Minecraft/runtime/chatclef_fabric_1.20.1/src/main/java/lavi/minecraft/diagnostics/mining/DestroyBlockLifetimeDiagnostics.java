package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260807_kpopmodder: Emit DestroyBlockTask START/STOP lifetime events only.
final class DestroyBlockLifetimeDiagnostics {
    private DestroyBlockLifetimeDiagnostics() {
    }

    static void logStart(AltoClef mod,
                         DestroyBlockTask task,
                         BlockPos target,
                         String forceCancelSource,
                         BaritonePathDiagnosticSnapshot cancelBefore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        DestroyBlockDiagnosticState.State state = DestroyBlockDiagnosticState.initialize(mod, task);
        DestroyBlockDiagnosticState.updateObservedProgress(mod, target, state);
        BaritonePathDiagnosticSnapshot cancelAfter = BaritonePathDiagnosticSnapshot.capture(mod, target, null, "unavailable");
        emit("START", mod, task, target, null, forceCancelSource, state, cancelBefore, cancelAfter);
    }

    static void logStop(AltoClef mod,
                        DestroyBlockTask task,
                        BlockPos target,
                        Task interruptTask,
                        String forceCancelSource,
                        BaritonePathDiagnosticSnapshot cancelBefore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        DestroyBlockDiagnosticState.State state = DestroyBlockDiagnosticState.getOrCreate(task);
        DestroyBlockDiagnosticState.updateObservedProgress(mod, target, state);
        BaritonePathDiagnosticSnapshot cancelAfter = BaritonePathDiagnosticSnapshot.capture(mod, target, null, "unavailable");
        emit("STOP", mod, task, target, interruptTask, forceCancelSource, state, cancelBefore, cancelAfter);
    }

    private static void emit(String phase,
                             AltoClef mod,
                             DestroyBlockTask task,
                             BlockPos target,
                             Task interruptTask,
                             String forceCancelSource,
                             DestroyBlockDiagnosticState.State state,
                             BaritonePathDiagnosticSnapshot cancelBefore,
                             BaritonePathDiagnosticSnapshot cancelAfter) {
        long tick = ChatClefDiagnostics.currentClientTickId();
        int cobblestoneAtStop = "STOP".equals(phase) ? DestroyBlockDiagnosticFields.cobblestoneCount(mod) : -1;
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "DESTROY_BLOCK_LIFETIME",
                phase,
                ChatClefDiagnostics.blockPos(target),
                Long.toString(state.runId),
                forceCancelSource
        );
        MiningDiagnosticEmitter.emit("DESTROY_BLOCK_LIFETIME", "destroy_block_lifetime", task,
                "destroy_lifetime|" + phase + "|" + System.identityHashCode(task),
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
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
                        cancelAfter == null ? new Object[0] : cancelAfter.fields("After")));
    }
}
