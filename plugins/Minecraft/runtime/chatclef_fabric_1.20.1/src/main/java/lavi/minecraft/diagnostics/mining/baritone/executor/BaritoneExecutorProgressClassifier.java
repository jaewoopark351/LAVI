package lavi.minecraft.diagnostics.mining.baritone.executor;

import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

import java.util.List;

//20260830_kpopmodder: Classify only already captured cheap executor observations.
final class BaritoneExecutorProgressClassifier {
    private static final double PLAYER_MOVEMENT_EPSILON = 0.02;
    private static final double TARGET_DISTANCE_EPSILON = 0.01;

    private BaritoneExecutorProgressClassifier() {
    }

    static void collectMaterialChanges(List<String> changes,
                                       BaritoneExecutorProgressSnapshot previous,
                                       BaritoneExecutorProgressSnapshot current) {
        addIfChanged(changes, "current_executor_identity_changed",
                previous.currentExecutorIdentity(), current.currentExecutorIdentity());
        addIfChanged(changes, "next_executor_identity_changed",
                previous.nextExecutorIdentity(), current.nextExecutorIdentity());
        addIfChanged(changes, "goal_identity_changed", previous.goalIdentity(), current.goalIdentity());
        addIfChanged(changes, "in_progress_identity_changed",
                previous.inProgressIdentity(), current.inProgressIdentity());
        addIfChanged(changes, "current_executor_position_changed",
                previous.currentExecutorPositionText(), current.currentExecutorPositionText());
        addIfChanged(changes, "current_executor_failed_changed",
                previous.currentExecutorFailed(), current.currentExecutorFailed());
        addIfChanged(changes, "current_executor_finished_changed",
                previous.currentExecutorFinished(), current.currentExecutorFinished());
        addIfChanged(changes, "cancel_requested_changed", previous.cancelRequested(), current.cancelRequested());
        addIfChanged(changes, "calc_failed_last_tick_changed",
                previous.calcFailedLastTick(), current.calcFailedLastTick());
    }

    static boolean executorAdvanced(BaritoneExecutorProgressSnapshot previous,
                                    BaritoneExecutorProgressSnapshot current) {
        if (previous == null
                || current == null
                || !previous.currentExecutorIdentity().equals(current.currentExecutorIdentity())
                || "none".equals(current.currentExecutorIdentity())) {
            return false;
        }
        return current.currentExecutorPosition() > previous.currentExecutorPosition();
    }

    static boolean movedEnough(double displacement) {
        return Double.isFinite(displacement) && displacement >= PLAYER_MOVEMENT_EPSILON;
    }

    static boolean targetDistanceImproved(BaritoneExecutorProgressSnapshot previous,
                                          BaritoneExecutorProgressSnapshot current) {
        if (previous == null || current == null
                || !previous.targetDistanceAvailable() || !current.targetDistanceAvailable()) {
            return false;
        }
        return current.targetDistanceSq() < previous.targetDistanceSq() - TARGET_DISTANCE_EPSILON;
    }

    static boolean targetDistanceChanged(BaritoneExecutorProgressSnapshot previous,
                                         BaritoneExecutorProgressSnapshot current) {
        if (previous == null || current == null
                || !previous.targetDistanceAvailable() || !current.targetDistanceAvailable()) {
            return false;
        }
        return Math.abs(current.targetDistanceSq() - previous.targetDistanceSq()) >= TARGET_DISTANCE_EPSILON;
    }

    static String reason(List<String> changes, boolean heartbeat) {
        if (heartbeat) {
            return "NO_PROGRESS_HEARTBEAT_200_TICKS_OR_10_SECONDS";
        }
        if (changes.contains("initial_snapshot")) {
            return "INITIAL_EXECUTOR_PROGRESS_SNAPSHOT";
        }
        if (changes.contains("executor_position_advanced")) {
            return "EXECUTOR_POSITION_ADVANCED";
        }
        if (changes.contains("player_moved")) {
            return "PLAYER_MOVED";
        }
        if (changes.contains("target_distance_improved")) {
            return "TARGET_DISTANCE_IMPROVED";
        }
        return "EXECUTOR_PROGRESS_STATE_CHANGED";
    }

    static String fingerprintCore(BaritoneExecutorProgressSnapshot snapshot,
                                  List<String> changes,
                                  boolean heartbeat) {
        return MiningDiagnosticEmitter.joinFingerprint(
                snapshot.pathingBehaviorIdentity(),
                snapshot.currentExecutorIdentity(),
                snapshot.nextExecutorIdentity(),
                snapshot.goalIdentity(),
                snapshot.inProgressIdentity(),
                snapshot.currentExecutorPositionText(),
                snapshot.currentExecutorFailed(),
                snapshot.currentExecutorFinished(),
                snapshot.cancelRequested(),
                snapshot.calcFailedLastTick(),
                String.join(",", changes),
                heartbeat ? "unchanged_progress_summary" : "state_change"
        );
    }

    private static void addIfChanged(List<String> changes, String field, String before, String after) {
        if (!String.valueOf(before).equals(String.valueOf(after))) {
            changes.add(field);
        }
    }
}
