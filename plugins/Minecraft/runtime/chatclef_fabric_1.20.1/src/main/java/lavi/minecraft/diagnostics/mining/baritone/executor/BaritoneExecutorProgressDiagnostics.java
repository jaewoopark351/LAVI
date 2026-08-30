package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;

//20260830_kpopmodder: Preserve executor-progress hooks as a thin diagnostics compatibility facade.
public final class BaritoneExecutorProgressDiagnostics {
    private BaritoneExecutorProgressDiagnostics() {
    }

    public static void logTickPathHead(PathingBehavior behavior,
                                       PathExecutor current,
                                       PathExecutor next,
                                       AbstractNodeCostSearch inProgress,
                                       Goal activeGoal,
                                       BetterBlockPos expectedSegmentStart,
                                       boolean cancelRequested,
                                       boolean calcFailedLastTick) {
        BaritoneExecutorProgressCoordinator.recordHead(
                behavior,
                current,
                next,
                inProgress,
                activeGoal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
    }

    public static void logTickPathReturn(PathingBehavior behavior,
                                         PathExecutor current,
                                         PathExecutor next,
                                         AbstractNodeCostSearch inProgress,
                                         Goal activeGoal,
                                         BetterBlockPos expectedSegmentStart,
                                         boolean cancelRequested,
                                         boolean calcFailedLastTick) {
        BaritoneExecutorProgressCoordinator.recordReturn(
                behavior,
                current,
                next,
                inProgress,
                activeGoal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
    }
}
