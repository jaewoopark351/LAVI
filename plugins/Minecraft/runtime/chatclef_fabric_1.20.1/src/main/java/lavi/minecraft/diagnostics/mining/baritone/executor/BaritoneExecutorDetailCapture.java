package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

//20260830_kpopmodder: Capture expensive passive executor details only after the event gate grants emission.
final class BaritoneExecutorDetailCapture {
    private BaritoneExecutorDetailCapture() {
    }

    static BaritoneExecutorDetailSnapshot capture(BaritoneExecutorProgressDetailSource source,
                                                   BaritoneExecutorProgressObservation observation) {
        return new BaritoneExecutorDetailSnapshot(
                BaritoneExecutorSnapshotValues.safeValue(
                        () -> source.behavior != null && source.behavior.isSafeToCancel()),
                BaritoneExecutorSnapshotValues.safeValue(() -> source.behavior == null
                        ? "unavailable"
                        : source.behavior.estimatedTicksToGoal().map(Object::toString).orElse("empty")),
                BaritoneExecutorSnapshotValues.safeValue(() -> source.expectedSegmentStart),
                BaritoneExecutorSnapshotValues.className(source.activeGoal),
                BaritoneExecutorSnapshotValues.summarizeObject(source.activeGoal),
                BaritoneExecutorSnapshotValues.className(source.inProgress),
                BaritoneExecutorSnapshotValues.summarizeFinder(source.inProgress),
                captureCurrentExecutor(source.current, observation),
                captureExecutor(source.next),
                capturePlayer(source),
                BaritoneExecutorTargetSnapshot.capture(
                        source.activeGoal,
                        observation.playerX(),
                        observation.playerY(),
                        observation.playerZ())
        );
    }

    private static BaritoneExecutorStateSnapshot captureCurrentExecutor(
            PathExecutor executor,
            BaritoneExecutorProgressObservation observation) {
        IPath path = path(executor);
        return executorSnapshot(
                observation.currentExecutorPresent(),
                observation.currentExecutorIdentity(),
                BaritoneExecutorSnapshotValues.className(executor),
                observation.currentExecutorPosition(),
                observation.currentExecutorPositionText(),
                observation.currentExecutorFailed(),
                observation.currentExecutorFinished(),
                path
        );
    }

    private static BaritoneExecutorStateSnapshot captureExecutor(PathExecutor executor) {
        IPath path = path(executor);
        int position = safePosition(executor);
        return executorSnapshot(
                executor != null,
                BaritoneExecutorSnapshotValues.identity(executor),
                BaritoneExecutorSnapshotValues.className(executor),
                position,
                position == BaritoneExecutorSnapshotValues.UNAVAILABLE_INT
                        ? "unavailable"
                        : Integer.toString(position),
                executor == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(executor::failed),
                executor == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(executor::finished),
                path
        );
    }

    private static BaritoneExecutorStateSnapshot executorSnapshot(boolean present,
                                                                  String identity,
                                                                  String type,
                                                                  int position,
                                                                  String positionText,
                                                                  String failed,
                                                                  String finished,
                                                                  IPath path) {
        return new BaritoneExecutorStateSnapshot(
                present,
                identity,
                type,
                position,
                positionText,
                failed,
                finished,
                BaritoneExecutorSnapshotValues.identity(path),
                BaritoneExecutorSnapshotValues.className(path),
                path == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(path::length),
                path == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(() -> path.movements().size()),
                path == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(() -> path.positions().size()),
                path == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(path::getSrc),
                path == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(path::getDest),
                path == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(path::getGoal)
        );
    }

    private static BaritonePlayerProgressSnapshot capturePlayer(BaritoneExecutorProgressDetailSource source) {
        ClientPlayerEntity player = source.player;
        Vec3d velocity = safeVelocity(player);
        return new BaritonePlayerProgressSnapshot(
                player != null,
                player == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(player::getPos),
                player == null ? "none" : BaritoneExecutorSnapshotValues.safeValue(player::getBlockPos),
                velocity == null ? "none" : ChatClefDiagnostics.vec3d(velocity),
                velocity == null ? "none" : BaritoneExecutorSnapshotValues.formatDouble(velocity.lengthSquared()),
                BaritoneExecutorSnapshotValues.safeValue(() -> source.client == null || source.client.world == null
                        ? "none"
                        : source.client.world.getRegistryKey().getValue()),
                player == null ? "none" : poseState(player)
        );
    }

    private static IPath path(PathExecutor executor) {
        if (executor == null) {
            return null;
        }
        try {
            return executor.getPath();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static int safePosition(PathExecutor executor) {
        if (executor == null) {
            return BaritoneExecutorSnapshotValues.UNAVAILABLE_INT;
        }
        try {
            return executor.getPosition();
        } catch (RuntimeException | LinkageError error) {
            return BaritoneExecutorSnapshotValues.UNAVAILABLE_INT;
        }
    }

    private static Vec3d safeVelocity(ClientPlayerEntity player) {
        if (player == null) {
            return null;
        }
        try {
            return player.getVelocity();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static String poseState(ClientPlayerEntity player) {
        return "onGround=" + BaritoneExecutorSnapshotValues.safeValue(player::isOnGround)
                + ",sneaking=" + BaritoneExecutorSnapshotValues.safeValue(player::isSneaking)
                + ",sprinting=" + BaritoneExecutorSnapshotValues.safeValue(player::isSprinting)
                + ",usingItem=" + BaritoneExecutorSnapshotValues.safeValue(player::isUsingItem);
    }
}
