package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Orchestrate cheap executor observations without owning state storage or event output.
final class BaritoneExecutorProgressCoordinator {
    private BaritoneExecutorProgressCoordinator() {
    }

    static void recordHead(PathingBehavior behavior,
                           PathExecutor current,
                           PathExecutor next,
                           AbstractNodeCostSearch inProgress,
                           Goal activeGoal,
                           BetterBlockPos expectedSegmentStart,
                           boolean cancelRequested,
                           boolean calcFailedLastTick) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        BaritoneExecutorProgressSnapshot snapshot = capture(
                "TICK_PATH_HEAD",
                behavior,
                current,
                next,
                inProgress,
                activeGoal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
        BaritoneExecutorProgressStateRegistry.stateFor(snapshot).recordHead(snapshot);
    }

    static void recordReturn(PathingBehavior behavior,
                             PathExecutor current,
                             PathExecutor next,
                             AbstractNodeCostSearch inProgress,
                             Goal activeGoal,
                             BetterBlockPos expectedSegmentStart,
                             boolean cancelRequested,
                             boolean calcFailedLastTick) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        BaritoneExecutorProgressSnapshot snapshot = capture(
                "TICK_PATH_RETURN",
                behavior,
                current,
                next,
                inProgress,
                activeGoal,
                expectedSegmentStart,
                cancelRequested,
                calcFailedLastTick
        );
        BaritoneExecutorProgressState.Emission emission =
                BaritoneExecutorProgressStateRegistry.stateFor(snapshot).recordReturn(snapshot);
        if (emission.emit()) {
            BaritoneExecutorProgressEventEmitter.emit(snapshot, emission);
        }
    }

    private static BaritoneExecutorProgressSnapshot capture(String phase,
                                                             PathingBehavior behavior,
                                                             PathExecutor current,
                                                             PathExecutor next,
                                                             AbstractNodeCostSearch inProgress,
                                                             Goal activeGoal,
                                                             BetterBlockPos expectedSegmentStart,
                                                             boolean cancelRequested,
                                                             boolean calcFailedLastTick) {
        return BaritoneExecutorProgressSnapshot.capture(
                phase,
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
