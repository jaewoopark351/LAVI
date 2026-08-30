package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.progress.MovementProgressDiagnosticFields;
import lavi.minecraft.diagnostics.mining.progress.MovementProgressSemanticFingerprint;
import net.minecraft.util.math.BlockPos;

//20260806_kpopmodder: Observe existing MovementProgressChecker results without changing checker timing.
final class MovementProgressDiagnostics {
    private MovementProgressDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    BlockPos target,
                    String checkerOwner,
                    int checkerCallIndex,
                    boolean checkEvaluated,
                    boolean checkResult,
                    boolean resetObservedBeforeCheck,
                    String resetReason,
                    String failureTransition,
                    boolean moveCheckFirstEvaluated,
                    Object moveCheckFirstResult,
                    boolean stuckCheckEvaluated,
                    Object stuckCheckResult,
                    boolean moveCheckSecondEvaluated,
                    Object moveCheckSecondResult) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = MovementProgressSemanticFingerprint.create(
                target, checkerOwner, checkerCallIndex, checkEvaluated, checkResult,
                resetObservedBeforeCheck, resetReason, failureTransition);
        MiningDiagnosticEmitter.emitLazy("MOVEMENT_PROGRESS_CHECK_RESULT", "movement_progress_check_result", task,
                "movement_progress|" + checkerOwner + "|" + checkerCallIndex + "|" + System.identityHashCode(task),
                fingerprint,
                () -> MovementProgressDiagnosticFields.capture(mod, target, checkerOwner, checkerCallIndex,
                        checkEvaluated, checkResult, resetObservedBeforeCheck, resetReason, failureTransition,
                        moveCheckFirstEvaluated, moveCheckFirstResult, stuckCheckEvaluated, stuckCheckResult,
                        moveCheckSecondEvaluated, moveCheckSecondResult));
    }
}
