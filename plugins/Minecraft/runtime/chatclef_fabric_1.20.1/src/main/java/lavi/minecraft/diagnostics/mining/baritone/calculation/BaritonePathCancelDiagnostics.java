package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.goals.Goal;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritonePathCancelDiagnostics {
    private BaritonePathCancelDiagnostics() {
    }

    public static void log(PathingBehavior behavior,
                           String phase,
                           PathExecutor current,
                           PathExecutor next,
                           AbstractNodeCostSearch inProgress,
                           Goal activeGoal,
                           boolean cancelRequested,
                           boolean calcFailedLastTick) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String behaviorIdentity = BaritonePathObjectFormatters.identity(behavior);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PATHING_FORCE_CANCEL_BOUNDARY",
                behaviorIdentity,
                phase,
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next),
                BaritonePathObjectFormatters.identity(inProgress),
                Boolean.toString(cancelRequested),
                Boolean.toString(calcFailedLastTick)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_PATHING_FORCE_CANCEL_BOUNDARY",
                "baritone_pathing_force_cancel_boundary",
                "baritone_pathing_behavior_observer",
                "pathing_behavior_force_cancel_" + phase,
                "baritone_force_cancel|" + behaviorIdentity,
                fingerprint,
                () -> new Object[]{
                        "phase", phase,
                        "pathingBehaviorIdentity", behaviorIdentity,
                        "pathingGoalType", BaritonePathObjectFormatters.className(activeGoal),
                        "pathingGoalSummary", BaritonePathObjectFormatters.summarizeGoal(activeGoal),
                        "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                        "cancelRequested", cancelRequested,
                        "calcFailedLastTick", calcFailedLastTick
                }
        );
    }
}
