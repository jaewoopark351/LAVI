package lavi.minecraft.diagnostics.mining.baritone.executor;

import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

import java.util.ArrayList;
import java.util.List;

final class BaritoneExecutorProgressState {
    private static final double PLAYER_MOVEMENT_EPSILON = 0.02;
    private static final double TARGET_DISTANCE_EPSILON = 0.01;
    private static final long NO_PROGRESS_HEARTBEAT_TICKS = 100;

    private BaritoneExecutorProgressSnapshot headSnapshot;
    private BaritoneExecutorProgressSnapshot lastReturnSnapshot;
    private long lastExecutorAdvanceTick = -1;
    private long lastPlayerMovementTick = -1;
    private long lastTargetDistanceImprovementTick = -1;
    private long lastHeartbeatTick = -1;
    private int localRepeatCount;

    synchronized void recordHead(BaritoneExecutorProgressSnapshot snapshot) {
        headSnapshot = snapshot;
    }

    synchronized Emission recordReturn(BaritoneExecutorProgressSnapshot snapshot) {
        BaritoneExecutorProgressSnapshot head = headSnapshot;
        headSnapshot = null;

        List<String> changes = new ArrayList<>();
        boolean initial = lastReturnSnapshot == null;
        if (initial) {
            if (!snapshot.hasRelevantState()) {
                localRepeatCount++;
                lastReturnSnapshot = snapshot;
                return Emission.suppressed();
            }
            changes.add("initial_snapshot");
            initializeProgressTicks(snapshot);
        } else {
            collectMaterialChanges(changes, lastReturnSnapshot, snapshot);
        }

        boolean executorAdvanced = executorAdvanced(head, snapshot) || executorAdvanced(lastReturnSnapshot, snapshot);
        boolean playerMoved = movedEnough(snapshot.playerDisplacementFrom(head))
                || movedEnough(snapshot.playerDisplacementFrom(lastReturnSnapshot));
        boolean targetImproved = targetDistanceImproved(head, snapshot)
                || targetDistanceImproved(lastReturnSnapshot, snapshot);

        if (executorAdvanced) {
            changes.add("executor_position_advanced");
            lastExecutorAdvanceTick = snapshot.tick();
        }
        if (changes.contains("current_executor_identity_changed") && snapshot.currentExecutorPresent()) {
            lastExecutorAdvanceTick = snapshot.tick();
        }
        if (playerMoved) {
            changes.add("player_moved");
            lastPlayerMovementTick = snapshot.tick();
        }
        if (lastPlayerMovementTick < 0 && snapshot.hasRelevantState()) {
            lastPlayerMovementTick = snapshot.tick();
        }
        if (targetImproved) {
            changes.add("target_distance_improved");
            lastTargetDistanceImprovementTick = snapshot.tick();
        } else if (targetDistanceChanged(head, snapshot) || targetDistanceChanged(lastReturnSnapshot, snapshot)) {
            changes.add("target_distance_changed_without_improvement");
        }
        if (lastTargetDistanceImprovementTick < 0 && snapshot.targetDistanceAvailable()) {
            lastTargetDistanceImprovementTick = snapshot.tick();
        }

        long ticksSinceExecutorAdvance = ticksSince(lastExecutorAdvanceTick, snapshot.tick());
        long ticksSincePlayerMovement = ticksSince(lastPlayerMovementTick, snapshot.tick());
        long ticksSinceTargetImprovement = ticksSince(lastTargetDistanceImprovementTick, snapshot.tick());
        long ticksSinceAnyProgress = ticksSinceAnyProgress(snapshot.tick());
        boolean heartbeat = false;
        if (changes.isEmpty()
                && snapshot.hasRelevantState()
                && ticksSinceAnyProgress >= NO_PROGRESS_HEARTBEAT_TICKS
                && (lastHeartbeatTick < 0 || snapshot.tick() - lastHeartbeatTick >= NO_PROGRESS_HEARTBEAT_TICKS)) {
            changes.add("no_progress_heartbeat_100_ticks");
            lastHeartbeatTick = snapshot.tick();
            heartbeat = true;
        }

        if (changes.isEmpty()) {
            localRepeatCount++;
            lastReturnSnapshot = snapshot;
            return Emission.suppressed();
        }

        int repeatCount = localRepeatCount;
        localRepeatCount = 0;
        Emission emission = Emission.emit(
                head,
                reason(changes, heartbeat),
                String.join(",", changes),
                repeatCount,
                ticksSinceExecutorAdvance,
                ticksSincePlayerMovement,
                ticksSinceTargetImprovement,
                ticksSinceAnyProgress,
                snapshot.executorPositionDeltaFrom(head),
                snapshot.executorPositionDeltaFrom(lastReturnSnapshot),
                BaritoneExecutorProgressSnapshot.formatDouble(snapshot.playerDisplacementFrom(head)),
                BaritoneExecutorProgressSnapshot.formatDouble(snapshot.playerDisplacementFrom(lastReturnSnapshot)),
                snapshot.targetDistanceDeltaFrom(head),
                snapshot.targetDistanceDeltaFrom(lastReturnSnapshot),
                fingerprintCore(snapshot, changes, heartbeat)
        );
        lastReturnSnapshot = snapshot;
        return emission;
    }

    private void collectMaterialChanges(List<String> changes,
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

    private void initializeProgressTicks(BaritoneExecutorProgressSnapshot snapshot) {
        lastExecutorAdvanceTick = snapshot.tick();
        lastPlayerMovementTick = snapshot.tick();
        if (snapshot.targetDistanceAvailable()) {
            lastTargetDistanceImprovementTick = snapshot.tick();
        }
    }

    private long ticksSinceAnyProgress(long tick) {
        long lastProgress = Math.max(lastExecutorAdvanceTick, lastPlayerMovementTick);
        lastProgress = Math.max(lastProgress, lastTargetDistanceImprovementTick);
        return ticksSince(lastProgress, tick);
    }

    private static boolean executorAdvanced(BaritoneExecutorProgressSnapshot previous,
                                            BaritoneExecutorProgressSnapshot current) {
        if (previous == null
                || current == null
                || !previous.currentExecutorIdentity().equals(current.currentExecutorIdentity())
                || "none".equals(current.currentExecutorIdentity())) {
            return false;
        }
        return current.currentExecutorPosition() > previous.currentExecutorPosition();
    }

    private static boolean movedEnough(double displacement) {
        return Double.isFinite(displacement) && displacement >= PLAYER_MOVEMENT_EPSILON;
    }

    private static boolean targetDistanceImproved(BaritoneExecutorProgressSnapshot previous,
                                                  BaritoneExecutorProgressSnapshot current) {
        if (previous == null || current == null
                || !previous.targetDistanceAvailable() || !current.targetDistanceAvailable()) {
            return false;
        }
        return current.targetDistanceSq() < previous.targetDistanceSq() - TARGET_DISTANCE_EPSILON;
    }

    private static boolean targetDistanceChanged(BaritoneExecutorProgressSnapshot previous,
                                                 BaritoneExecutorProgressSnapshot current) {
        if (previous == null || current == null
                || !previous.targetDistanceAvailable() || !current.targetDistanceAvailable()) {
            return false;
        }
        return Math.abs(current.targetDistanceSq() - previous.targetDistanceSq()) >= TARGET_DISTANCE_EPSILON;
    }

    private static long ticksSince(long tick, long currentTick) {
        if (tick < 0) {
            return -1;
        }
        return Math.max(0, currentTick - tick);
    }

    private static void addIfChanged(List<String> changes, String field, String before, String after) {
        if (!String.valueOf(before).equals(String.valueOf(after))) {
            changes.add(field);
        }
    }

    private static String reason(List<String> changes, boolean heartbeat) {
        if (heartbeat) {
            return "NO_PROGRESS_HEARTBEAT_100_TICKS";
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

    private static String fingerprintCore(BaritoneExecutorProgressSnapshot snapshot,
                                          List<String> changes,
                                          boolean heartbeat) {
        String heartbeatWindow = heartbeat ? Long.toString(snapshot.tick() / NO_PROGRESS_HEARTBEAT_TICKS) : "state_change";
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
                heartbeatWindow
        );
    }

    static final class Emission {
        private static final Emission SUPPRESSED = new Emission(false, null, "none", "none",
                0, -1, -1, -1, -1, "unavailable", "unavailable",
                "unavailable", "unavailable", "unavailable", "unavailable", "none");

        private final boolean emit;
        private final BaritoneExecutorProgressSnapshot headSnapshot;
        private final String reason;
        private final String changedFields;
        private final int localRepeatCount;
        private final long ticksSinceExecutorAdvance;
        private final long ticksSincePlayerMovement;
        private final long ticksSinceTargetDistanceImprovement;
        private final long ticksSinceAnyProgress;
        private final String executorPositionDeltaSinceHead;
        private final String executorPositionDeltaSinceLastReturn;
        private final String playerDisplacementSinceHead;
        private final String playerDisplacementSinceLastReturn;
        private final String targetDistanceDeltaSinceHead;
        private final String targetDistanceDeltaSinceLastReturn;
        private final String fingerprintCore;

        private Emission(boolean emit,
                         BaritoneExecutorProgressSnapshot headSnapshot,
                         String reason,
                         String changedFields,
                         int localRepeatCount,
                         long ticksSinceExecutorAdvance,
                         long ticksSincePlayerMovement,
                         long ticksSinceTargetDistanceImprovement,
                         long ticksSinceAnyProgress,
                         String executorPositionDeltaSinceHead,
                         String executorPositionDeltaSinceLastReturn,
                         String playerDisplacementSinceHead,
                         String playerDisplacementSinceLastReturn,
                         String targetDistanceDeltaSinceHead,
                         String targetDistanceDeltaSinceLastReturn,
                         String fingerprintCore) {
            this.emit = emit;
            this.headSnapshot = headSnapshot;
            this.reason = reason;
            this.changedFields = changedFields;
            this.localRepeatCount = localRepeatCount;
            this.ticksSinceExecutorAdvance = ticksSinceExecutorAdvance;
            this.ticksSincePlayerMovement = ticksSincePlayerMovement;
            this.ticksSinceTargetDistanceImprovement = ticksSinceTargetDistanceImprovement;
            this.ticksSinceAnyProgress = ticksSinceAnyProgress;
            this.executorPositionDeltaSinceHead = executorPositionDeltaSinceHead;
            this.executorPositionDeltaSinceLastReturn = executorPositionDeltaSinceLastReturn;
            this.playerDisplacementSinceHead = playerDisplacementSinceHead;
            this.playerDisplacementSinceLastReturn = playerDisplacementSinceLastReturn;
            this.targetDistanceDeltaSinceHead = targetDistanceDeltaSinceHead;
            this.targetDistanceDeltaSinceLastReturn = targetDistanceDeltaSinceLastReturn;
            this.fingerprintCore = fingerprintCore;
        }

        static Emission suppressed() {
            return SUPPRESSED;
        }

        static Emission emit(BaritoneExecutorProgressSnapshot headSnapshot,
                             String reason,
                             String changedFields,
                             int localRepeatCount,
                             long ticksSinceExecutorAdvance,
                             long ticksSincePlayerMovement,
                             long ticksSinceTargetDistanceImprovement,
                             long ticksSinceAnyProgress,
                             String executorPositionDeltaSinceHead,
                             String executorPositionDeltaSinceLastReturn,
                             String playerDisplacementSinceHead,
                             String playerDisplacementSinceLastReturn,
                             String targetDistanceDeltaSinceHead,
                             String targetDistanceDeltaSinceLastReturn,
                             String fingerprintCore) {
            return new Emission(true, headSnapshot, reason, changedFields, localRepeatCount,
                    ticksSinceExecutorAdvance, ticksSincePlayerMovement, ticksSinceTargetDistanceImprovement,
                    ticksSinceAnyProgress, executorPositionDeltaSinceHead, executorPositionDeltaSinceLastReturn,
                    playerDisplacementSinceHead, playerDisplacementSinceLastReturn, targetDistanceDeltaSinceHead,
                    targetDistanceDeltaSinceLastReturn, fingerprintCore);
        }

        boolean emit() {
            return emit;
        }

        BaritoneExecutorProgressSnapshot headSnapshot() {
            return headSnapshot;
        }

        String reason() {
            return reason;
        }

        String changedFields() {
            return changedFields;
        }

        int localRepeatCount() {
            return localRepeatCount;
        }

        long ticksSinceExecutorAdvance() {
            return ticksSinceExecutorAdvance;
        }

        long ticksSincePlayerMovement() {
            return ticksSincePlayerMovement;
        }

        long ticksSinceTargetDistanceImprovement() {
            return ticksSinceTargetDistanceImprovement;
        }

        long ticksSinceAnyProgress() {
            return ticksSinceAnyProgress;
        }

        String executorPositionDeltaSinceHead() {
            return executorPositionDeltaSinceHead;
        }

        String executorPositionDeltaSinceLastReturn() {
            return executorPositionDeltaSinceLastReturn;
        }

        String playerDisplacementSinceHead() {
            return playerDisplacementSinceHead;
        }

        String playerDisplacementSinceLastReturn() {
            return playerDisplacementSinceLastReturn;
        }

        String targetDistanceDeltaSinceHead() {
            return targetDistanceDeltaSinceHead;
        }

        String targetDistanceDeltaSinceLastReturn() {
            return targetDistanceDeltaSinceLastReturn;
        }

        String fingerprint(String eventName) {
            return MiningDiagnosticEmitter.joinFingerprint(eventName, reason, fingerprintCore);
        }
    }
}
