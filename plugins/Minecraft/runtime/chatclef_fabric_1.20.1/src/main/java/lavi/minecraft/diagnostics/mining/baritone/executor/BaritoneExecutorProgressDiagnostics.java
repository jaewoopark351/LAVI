package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//20260814_kpopmodder: Observe Baritone executor progress without changing pathing, goal, input, or retry behavior.
public final class BaritoneExecutorProgressDiagnostics {
    private static final String EVENT_NAME = "BARITONE_EXECUTOR_PROGRESS_SNAPSHOT";
    private static final String EVENT_REASON = "baritone_executor_progress_snapshot";
    private static final String OWNER = "baritone_pathing_behavior_executor_observer";
    private static final int STATE_HARD_CAP = 256;
    private static final ConcurrentMap<String, BaritoneExecutorProgressState> STATES = new ConcurrentHashMap<>();

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
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        BaritoneExecutorProgressSnapshot snapshot = BaritoneExecutorProgressSnapshot.capture(
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
        stateFor(snapshot).recordHead(snapshot);
    }

    public static void logTickPathReturn(PathingBehavior behavior,
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
        BaritoneExecutorProgressSnapshot snapshot = BaritoneExecutorProgressSnapshot.capture(
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
        BaritoneExecutorProgressState.Emission emission = stateFor(snapshot).recordReturn(snapshot);
        if (!emission.emit()) {
            return;
        }
        emit(snapshot, emission);
    }

    private static void emit(BaritoneExecutorProgressSnapshot snapshot,
                             BaritoneExecutorProgressState.Emission emission) {
        String bucket = "baritone_executor_progress|" + snapshot.pathingBehaviorIdentity();
        String fingerprint = emission.fingerprint(EVENT_NAME);
        MiningDiagnosticEmitter.emit(
                EVENT_NAME,
                EVENT_REASON,
                null,
                bucket,
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
                                "owner", OWNER,
                                "trigger", "pathing_behavior_tick_path_return",
                                "correlation", bucket,
                                "progressReason", emission.reason(),
                                "changedFields", emission.changedFields(),
                                "localRepeatCount", emission.localRepeatCount(),
                                "ticksSinceExecutorAdvance", emission.ticksSinceExecutorAdvance(),
                                "ticksSincePlayerMovement", emission.ticksSincePlayerMovement(),
                                "ticksSinceTargetDistanceImprovement", emission.ticksSinceTargetDistanceImprovement(),
                                "ticksSinceAnyProgress", emission.ticksSinceAnyProgress(),
                                "executorPositionDeltaSinceHead", emission.executorPositionDeltaSinceHead(),
                                "executorPositionDeltaSinceLastReturn", emission.executorPositionDeltaSinceLastReturn(),
                                "playerDisplacementSinceHead", emission.playerDisplacementSinceHead(),
                                "playerDisplacementSinceLastReturn", emission.playerDisplacementSinceLastReturn(),
                                "targetDistanceDeltaSinceHead", emission.targetDistanceDeltaSinceHead(),
                                "targetDistanceDeltaSinceLastReturn", emission.targetDistanceDeltaSinceLastReturn()
                        },
                        BaritoneExecutorProgressSnapshot.fieldsOrUnavailable(emission.headSnapshot(), "Before"),
                        snapshot.fields("After"))
        );
    }

    private static BaritoneExecutorProgressState stateFor(BaritoneExecutorProgressSnapshot snapshot) {
        if (STATES.size() > STATE_HARD_CAP) {
            STATES.clear();
        }
        return STATES.computeIfAbsent(snapshot.pathingBehaviorIdentity(),
                ignored -> new BaritoneExecutorProgressState());
    }
}
