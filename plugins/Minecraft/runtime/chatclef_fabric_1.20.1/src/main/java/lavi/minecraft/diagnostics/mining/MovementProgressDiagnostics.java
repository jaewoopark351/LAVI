package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
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
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "MOVEMENT_PROGRESS_CHECK_RESULT",
                checkerOwner,
                Integer.toString(checkerCallIndex),
                ChatClefDiagnostics.blockPos(target),
                Boolean.toString(checkEvaluated),
                Boolean.toString(checkResult),
                failureTransition
        );
        MiningDiagnosticEmitter.emit("MOVEMENT_PROGRESS_CHECK_RESULT", "movement_progress_check_result", task,
                "movement_progress|" + checkerOwner + "|" + checkerCallIndex + "|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "movement_progress_checker_observer",
                        "trigger", "progress_check_returned",
                        "checkerOwner", checkerOwner,
                        "checkerCallIndex", checkerCallIndex,
                        "targetPosition", ChatClefDiagnostics.blockPos(target),
                        "checkEvaluated", checkEvaluated,
                        "checkResult", checkResult,
                        "failureTransition", failureTransition,
                        "mode", "existing_checker",
                        "lastResetTick", "unavailable_without_checker_internals",
                        "elapsedTicksSinceReset", "unavailable_without_checker_internals",
                        "resetReason", "unavailable_without_checker_internals",
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "startPlayerPosition", "unavailable_without_checker_internals",
                        "playerDisplacementSinceReset", "unavailable_without_checker_internals",
                        "controllerBreakingBlock", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getControllerExtras().isBreakingBlock()),
                        "breakingBlockPosition", ChatClefDiagnostics.safeValue(() -> mod == null || !mod.getControllerExtras().isBreakingBlock() ? "none" : mod.getControllerExtras().getBreakingBlockPos()),
                        "breakingProgress", ChatClefDiagnostics.safeValue(() -> mod == null ? "unavailable" : mod.getControllerExtras().getBreakingBlockProgress()),
                        "lastBreakingBlockPosition", "unavailable_without_checker_internals",
                        "lastBreakingBlockNowAir", "unavailable_without_checker_internals",
                        "distanceTimeoutSeconds", "6",
                        "minimumDistance", "0.1",
                        "mineTimeoutSeconds", "0.5",
                        "minimumMineProgress", "0.001",
                        "allowedAttempts", "1",
                        "moveCheckFirstEvaluated", moveCheckFirstEvaluated,
                        "moveCheckFirstResult", moveCheckFirstResult,
                        "stuckCheckEvaluated", stuckCheckEvaluated,
                        "stuckCheckResult", stuckCheckResult,
                        "moveCheckSecondEvaluated", moveCheckSecondEvaluated,
                        "moveCheckSecondResult", moveCheckSecondResult
                });
    }
}
