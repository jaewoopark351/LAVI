package lavi.minecraft.diagnostics.mining.baritone.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

//20260830_kpopmodder: Own only mutable executor-progress sequencing and heartbeat state.
final class BaritoneExecutorProgressStateMachine {
    private final LongSupplier monotonicNanos;

    private BaritoneExecutorProgressSnapshot headSnapshot;
    private BaritoneExecutorProgressSnapshot lastReturnSnapshot;
    private long lastExecutorAdvanceTick = -1;
    private long lastPlayerMovementTick = -1;
    private long lastTargetDistanceImprovementTick = -1;
    private long lastHeartbeatTick = -1;
    private long lastHeartbeatNanos = -1;
    private long lastSemanticProgressNanos = -1;
    private int localRepeatCount;

    BaritoneExecutorProgressStateMachine(LongSupplier monotonicNanos) {
        this.monotonicNanos = monotonicNanos;
    }

    synchronized void recordHead(BaritoneExecutorProgressSnapshot snapshot) {
        headSnapshot = snapshot;
    }

    synchronized BaritoneExecutorProgressEmission recordReturn(BaritoneExecutorProgressSnapshot snapshot) {
        long nowNanos = monotonicNanos.getAsLong();
        BaritoneExecutorProgressSnapshot head = headSnapshot;
        headSnapshot = null;

        List<String> changes = new ArrayList<>();
        boolean initial = lastReturnSnapshot == null;
        if (initial) {
            if (!snapshot.hasRelevantState()) {
                localRepeatCount++;
                lastReturnSnapshot = snapshot;
                return BaritoneExecutorProgressEmission.suppressed();
            }
            changes.add("initial_snapshot");
            initializeProgressTicks(snapshot, nowNanos);
        } else {
            BaritoneExecutorProgressClassifier.collectMaterialChanges(changes, lastReturnSnapshot, snapshot);
        }

        boolean executorAdvanced = BaritoneExecutorProgressClassifier.executorAdvanced(head, snapshot)
                || BaritoneExecutorProgressClassifier.executorAdvanced(lastReturnSnapshot, snapshot);
        boolean playerMoved = BaritoneExecutorProgressClassifier.movedEnough(snapshot.playerDisplacementFrom(head))
                || BaritoneExecutorProgressClassifier.movedEnough(
                        snapshot.playerDisplacementFrom(lastReturnSnapshot));
        boolean targetImproved = BaritoneExecutorProgressClassifier.targetDistanceImproved(head, snapshot)
                || BaritoneExecutorProgressClassifier.targetDistanceImproved(lastReturnSnapshot, snapshot);

        if (executorAdvanced) {
            changes.add("executor_position_advanced");
            lastExecutorAdvanceTick = snapshot.tick();
        }
        boolean executorIdentityBecamePresent = changes.contains("current_executor_identity_changed")
                && snapshot.currentExecutorPresent();
        if (executorIdentityBecamePresent) {
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
        } else if (BaritoneExecutorProgressClassifier.targetDistanceChanged(head, snapshot)
                || BaritoneExecutorProgressClassifier.targetDistanceChanged(lastReturnSnapshot, snapshot)) {
            changes.add("target_distance_changed_without_improvement");
        }
        if (lastTargetDistanceImprovementTick < 0 && snapshot.targetDistanceAvailable()) {
            lastTargetDistanceImprovementTick = snapshot.tick();
        }
        if (executorAdvanced || executorIdentityBecamePresent || playerMoved || targetImproved) {
            lastSemanticProgressNanos = nowNanos;
        }

        long ticksSinceExecutorAdvance = ticksSince(lastExecutorAdvanceTick, snapshot.tick());
        long ticksSincePlayerMovement = ticksSince(lastPlayerMovementTick, snapshot.tick());
        long ticksSinceTargetImprovement = ticksSince(lastTargetDistanceImprovementTick, snapshot.tick());
        long ticksSinceAnyProgress = ticksSinceAnyProgress(snapshot.tick());
        boolean heartbeat = false;
        if (changes.isEmpty()
                && snapshot.hasRelevantState()
                && BaritoneExecutorSamplingPolicy.heartbeatDue(
                        ticksSinceAnyProgress,
                        snapshot.tick(),
                        lastHeartbeatTick,
                        nowNanos,
                        lastHeartbeatNanos,
                        lastSemanticProgressNanos)) {
            changes.add("no_progress_heartbeat_200_ticks_or_10_seconds");
            lastHeartbeatTick = snapshot.tick();
            lastHeartbeatNanos = nowNanos;
            heartbeat = true;
        }

        if (changes.isEmpty()) {
            localRepeatCount++;
            lastReturnSnapshot = snapshot;
            return BaritoneExecutorProgressEmission.suppressed();
        }

        int repeatCount = localRepeatCount;
        localRepeatCount = 0;
        BaritoneExecutorProgressEmission emission = BaritoneExecutorProgressEmission.emit(
                head,
                BaritoneExecutorProgressClassifier.reason(changes, heartbeat),
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
                BaritoneExecutorProgressClassifier.fingerprintCore(snapshot, changes, heartbeat)
        );
        lastReturnSnapshot = snapshot;
        return emission;
    }

    private void initializeProgressTicks(BaritoneExecutorProgressSnapshot snapshot, long nowNanos) {
        lastExecutorAdvanceTick = snapshot.tick();
        lastPlayerMovementTick = snapshot.tick();
        lastSemanticProgressNanos = nowNanos;
        if (snapshot.targetDistanceAvailable()) {
            lastTargetDistanceImprovementTick = snapshot.tick();
        }
    }

    private long ticksSinceAnyProgress(long tick) {
        long lastProgress = Math.max(lastExecutorAdvanceTick, lastPlayerMovementTick);
        lastProgress = Math.max(lastProgress, lastTargetDistanceImprovementTick);
        return ticksSince(lastProgress, tick);
    }

    private static long ticksSince(long tick, long currentTick) {
        if (tick < 0) {
            return -1;
        }
        return Math.max(0, currentTick - tick);
    }
}
