package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.cancel.PreCancelBaritoneState;
import net.minecraft.util.math.BlockPos;

//20260807_kpopmodder: Emit DestroyBlockTask START/STOP lifetime events only.
final class DestroyBlockLifetimeDiagnostics {
    private DestroyBlockLifetimeDiagnostics() {
    }

    static void logStart(AltoClef mod,
                         DestroyBlockTask task,
                         BlockPos target,
                         String forceCancelSource,
                         PreCancelBaritoneState cancelBefore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        DestroyBlockDiagnosticState.State state = DestroyBlockDiagnosticState.initialize(mod, task);
        emit("START", mod, task, target, null, forceCancelSource, state, cancelBefore);
    }

    static void logStop(AltoClef mod,
                        DestroyBlockTask task,
                        BlockPos target,
                        Task interruptTask,
                        String forceCancelSource,
                        PreCancelBaritoneState cancelBefore) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        DestroyBlockDiagnosticState.State state = DestroyBlockDiagnosticState.getOrCreate(task);
        emit("STOP", mod, task, target, interruptTask, forceCancelSource, state, cancelBefore);
    }

    private static void emit(String phase,
                             AltoClef mod,
                             DestroyBlockTask task,
                             BlockPos target,
                             Task interruptTask,
                             String forceCancelSource,
                             DestroyBlockDiagnosticState.State state,
                             PreCancelBaritoneState cancelBefore) {
        long tick = ChatClefDiagnostics.currentClientTickId();
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "DESTROY_BLOCK_LIFETIME",
                phase,
                ChatClefDiagnostics.blockPos(target),
                Long.toString(state.runId),
                forceCancelSource
        );
        boolean terminal = "STOP".equals(phase);
        if (terminal) {
            MiningDiagnosticEmitter.emitReservedLazy("DESTROY_BLOCK_LIFETIME", "destroy_block_lifetime", task,
                    "destroy_lifetime|" + phase + "|" + System.identityHashCode(task),
                    fingerprint,
                    true,
                    () -> DestroyBlockLifetimeEventFields.capture(
                            phase, mod, task, target, interruptTask, forceCancelSource,
                            state, cancelBefore, tick));
            return;
        }
        MiningDiagnosticEmitter.emitLazy("DESTROY_BLOCK_LIFETIME", "destroy_block_lifetime", task,
                "destroy_lifetime|" + phase + "|" + System.identityHashCode(task),
                fingerprint,
                () -> DestroyBlockLifetimeEventFields.capture(
                        phase, mod, task, target, interruptTask, forceCancelSource,
                        state, cancelBefore, tick));
    }
}
