package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

//20260830_kpopmodder: Capture only cheap progress signals and retain passive references for later detail.
final class BaritoneExecutorSnapshotCapture {
    private BaritoneExecutorSnapshotCapture() {
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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        Vec3d playerPosition = safePlayerPosition(player);
        double playerX = playerPosition == null ? Double.NaN : playerPosition.x;
        double playerY = playerPosition == null ? Double.NaN : playerPosition.y;
        double playerZ = playerPosition == null ? Double.NaN : playerPosition.z;
        int currentPosition = safeExecutorPosition(current);

        BaritoneExecutorProgressObservation observation = new BaritoneExecutorProgressObservation(
                phase,
                ChatClefDiagnostics.currentClientTickId(),
                Thread.currentThread().getName(),
                BaritoneExecutorSnapshotValues.identity(behavior),
                BaritoneExecutorSnapshotValues.className(behavior),
                BaritoneExecutorSnapshotValues.safeValue(() -> behavior != null && behavior.isPathing()),
                Boolean.toString(cancelRequested),
                Boolean.toString(calcFailedLastTick),
                BaritoneExecutorSnapshotValues.identity(activeGoal),
                BaritoneExecutorSnapshotValues.identity(inProgress),
                BaritoneExecutorSnapshotValues.identity(current),
                BaritoneExecutorSnapshotValues.identity(next),
                current != null,
                currentPosition,
                currentPosition == BaritoneExecutorSnapshotValues.UNAVAILABLE_INT
                        ? "unavailable"
                        : Integer.toString(currentPosition),
                current == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(current::failed),
                current == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(current::finished),
                playerX,
                playerY,
                playerZ,
                BaritoneExecutorTargetObservation.distanceSq(activeGoal, playerX, playerY, playerZ)
        );
        BaritoneExecutorProgressDetailSource detailSource = new BaritoneExecutorProgressDetailSource(
                behavior,
                current,
                next,
                inProgress,
                activeGoal,
                expectedSegmentStart,
                client,
                player
        );
        return new BaritoneExecutorProgressSnapshot(observation, detailSource);
    }

    private static int safeExecutorPosition(PathExecutor executor) {
        if (executor == null) {
            return BaritoneExecutorSnapshotValues.UNAVAILABLE_INT;
        }
        try {
            return executor.getPosition();
        } catch (RuntimeException | LinkageError error) {
            return BaritoneExecutorSnapshotValues.UNAVAILABLE_INT;
        }
    }

    private static Vec3d safePlayerPosition(ClientPlayerEntity player) {
        if (player == null) {
            return null;
        }
        try {
            return player.getPos();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }
}
