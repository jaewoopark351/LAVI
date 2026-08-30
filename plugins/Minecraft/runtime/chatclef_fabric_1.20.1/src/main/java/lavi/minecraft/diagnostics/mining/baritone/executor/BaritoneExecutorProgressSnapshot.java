package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;

import java.util.function.Supplier;

//20260830_kpopmodder: Preserve the executor snapshot API while focused collaborators capture and project it.
final class BaritoneExecutorProgressSnapshot {
    private final BaritoneExecutorProgressObservation observation;
    private final BaritoneExecutorProgressDetailSource detailSource;

    BaritoneExecutorProgressSnapshot(BaritoneExecutorProgressObservation observation,
                                    BaritoneExecutorProgressDetailSource detailSource) {
        this.observation = observation;
        this.detailSource = detailSource;
    }

    static BaritoneExecutorProgressSnapshot capture(String phase,
                                                     PathingBehavior behavior,
                                                     PathExecutor current,
                                                     PathExecutor next,
                                                     AbstractNodeCostSearch inProgress,
                                                     Goal activeGoal,
                                                     BetterBlockPos expectedSegmentStart,
                                                     boolean cancelRequested,
                                                     boolean calcFailedLastTick) {
        return BaritoneExecutorSnapshotCapture.capture(
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

    static Object[] fieldsOrUnavailable(BaritoneExecutorProgressSnapshot snapshot, String suffix) {
        if (snapshot == null) {
            return new Object[]{"snapshotPresent" + suffix, false};
        }
        return snapshot.fields(suffix);
    }

    Object[] fields(String suffix) {
        BaritoneExecutorDetailSnapshot detail = BaritoneExecutorDetailCapture.capture(detailSource, observation);
        return BaritoneExecutorSnapshotFields.project(observation, detail, suffix);
    }

    boolean hasRelevantState() {
        return observation.hasRelevantState();
    }

    long tick() {
        return observation.tick();
    }

    String pathingBehaviorIdentity() {
        return observation.pathingBehaviorIdentity();
    }

    String currentExecutorIdentity() {
        return observation.currentExecutorIdentity();
    }

    String nextExecutorIdentity() {
        return observation.nextExecutorIdentity();
    }

    String goalIdentity() {
        return observation.goalIdentity();
    }

    String inProgressIdentity() {
        return observation.inProgressIdentity();
    }

    String currentExecutorFailed() {
        return observation.currentExecutorFailed();
    }

    String currentExecutorFinished() {
        return observation.currentExecutorFinished();
    }

    String cancelRequested() {
        return observation.cancelRequested();
    }

    String calcFailedLastTick() {
        return observation.calcFailedLastTick();
    }

    int currentExecutorPosition() {
        return observation.currentExecutorPosition();
    }

    String currentExecutorPositionText() {
        return observation.currentExecutorPositionText();
    }

    boolean currentExecutorPresent() {
        return observation.currentExecutorPresent();
    }

    boolean targetDistanceAvailable() {
        return observation.targetDistanceAvailable();
    }

    double targetDistanceSq() {
        return observation.targetDistanceSq();
    }

    double playerDisplacementFrom(BaritoneExecutorProgressSnapshot previous) {
        return BaritoneExecutorSnapshotMetrics.playerDisplacement(
                previous == null ? null : previous.observation,
                observation
        );
    }

    String executorPositionDeltaFrom(BaritoneExecutorProgressSnapshot previous) {
        return BaritoneExecutorSnapshotMetrics.executorPositionDelta(
                previous == null ? null : previous.observation,
                observation
        );
    }

    String targetDistanceDeltaFrom(BaritoneExecutorProgressSnapshot previous) {
        return BaritoneExecutorSnapshotMetrics.targetDistanceDelta(
                previous == null ? null : previous.observation,
                observation
        );
    }

    static String identity(Object value) {
        return BaritoneExecutorSnapshotValues.identity(value);
    }

    static String className(Object value) {
        return BaritoneExecutorSnapshotValues.className(value);
    }

    static String safeValue(Supplier<?> supplier) {
        return BaritoneExecutorSnapshotValues.safeValue(supplier);
    }

    static String formatDouble(double value) {
        return BaritoneExecutorSnapshotValues.formatDouble(value);
    }
}
