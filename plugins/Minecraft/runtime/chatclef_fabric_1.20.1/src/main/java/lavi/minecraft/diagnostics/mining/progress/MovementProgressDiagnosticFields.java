package lavi.minecraft.diagnostics.mining.progress;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Project movement progress detail only after the semantic gate admits it.
public final class MovementProgressDiagnosticFields {
    private MovementProgressDiagnosticFields() {
    }

    public static Object[] capture(AltoClef mod,
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
        return new Object[]{
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
                "resetObservedBeforeCheck", resetObservedBeforeCheck,
                "resetReason", normalize(resetReason),
                "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                "startPlayerPosition", "unavailable_without_checker_internals",
                "playerDisplacementSinceReset", "unavailable_without_checker_internals",
                "controllerBreakingBlock", ChatClefDiagnostics.safeValue(() ->
                        mod != null && mod.getControllerExtras().isBreakingBlock()),
                "breakingBlockPosition", ChatClefDiagnostics.safeValue(() ->
                        mod == null || !mod.getControllerExtras().isBreakingBlock()
                                ? "none"
                                : mod.getControllerExtras().getBreakingBlockPos()),
                "breakingProgress", ChatClefDiagnostics.safeValue(() ->
                        mod == null ? "unavailable" : mod.getControllerExtras().getBreakingBlockProgress()),
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
        };
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
