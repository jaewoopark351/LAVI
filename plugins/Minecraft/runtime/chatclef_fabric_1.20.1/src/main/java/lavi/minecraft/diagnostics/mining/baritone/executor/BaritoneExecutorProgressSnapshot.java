package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

final class BaritoneExecutorProgressSnapshot {
    private static final double UNAVAILABLE_DOUBLE = Double.NaN;
    private static final int UNAVAILABLE_INT = -1;

    private final String phase;
    private final long tick;
    private final String threadName;
    private final String pathingBehaviorIdentity;
    private final String pathingBehaviorType;
    private final String pathing;
    private final String safeToCancel;
    private final String estimatedTicksToGoal;
    private final String expectedSegmentStart;
    private final String cancelRequested;
    private final String calcFailedLastTick;
    private final String goalIdentity;
    private final String goalType;
    private final String goalSummary;
    private final String inProgressIdentity;
    private final String inProgressType;
    private final String inProgressSummary;
    private final ExecutorSnapshot currentExecutor;
    private final ExecutorSnapshot nextExecutor;
    private final PlayerSnapshot player;
    private final BaritoneExecutorTargetSnapshot target;

    private BaritoneExecutorProgressSnapshot(String phase,
                                             long tick,
                                             String threadName,
                                             String pathingBehaviorIdentity,
                                             String pathingBehaviorType,
                                             String pathing,
                                             String safeToCancel,
                                             String estimatedTicksToGoal,
                                             String expectedSegmentStart,
                                             String cancelRequested,
                                             String calcFailedLastTick,
                                             String goalIdentity,
                                             String goalType,
                                             String goalSummary,
                                             String inProgressIdentity,
                                             String inProgressType,
                                             String inProgressSummary,
                                             ExecutorSnapshot currentExecutor,
                                             ExecutorSnapshot nextExecutor,
                                             PlayerSnapshot player,
                                             BaritoneExecutorTargetSnapshot target) {
        this.phase = phase;
        this.tick = tick;
        this.threadName = threadName;
        this.pathingBehaviorIdentity = pathingBehaviorIdentity;
        this.pathingBehaviorType = pathingBehaviorType;
        this.pathing = pathing;
        this.safeToCancel = safeToCancel;
        this.estimatedTicksToGoal = estimatedTicksToGoal;
        this.expectedSegmentStart = expectedSegmentStart;
        this.cancelRequested = cancelRequested;
        this.calcFailedLastTick = calcFailedLastTick;
        this.goalIdentity = goalIdentity;
        this.goalType = goalType;
        this.goalSummary = goalSummary;
        this.inProgressIdentity = inProgressIdentity;
        this.inProgressType = inProgressType;
        this.inProgressSummary = inProgressSummary;
        this.currentExecutor = currentExecutor;
        this.nextExecutor = nextExecutor;
        this.player = player;
        this.target = target;
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
        PlayerSnapshot player = PlayerSnapshot.capture();
        return new BaritoneExecutorProgressSnapshot(
                phase,
                ChatClefDiagnostics.currentClientTickId(),
                Thread.currentThread().getName(),
                identity(behavior),
                className(behavior),
                safeValue(() -> behavior != null && behavior.isPathing()),
                safeValue(() -> behavior != null && behavior.isSafeToCancel()),
                safeValue(() -> behavior == null
                        ? "unavailable"
                        : behavior.estimatedTicksToGoal().map(Object::toString).orElse("empty")),
                safeValue(() -> expectedSegmentStart),
                Boolean.toString(cancelRequested),
                Boolean.toString(calcFailedLastTick),
                identity(activeGoal),
                className(activeGoal),
                summarizeObject(activeGoal),
                identity(inProgress),
                className(inProgress),
                summarizeFinder(inProgress),
                ExecutorSnapshot.capture(current),
                ExecutorSnapshot.capture(next),
                player,
                BaritoneExecutorTargetSnapshot.capture(activeGoal, player.x(), player.y(), player.z())
        );
    }

    static Object[] fieldsOrUnavailable(BaritoneExecutorProgressSnapshot snapshot, String suffix) {
        if (snapshot == null) {
            return new Object[]{"snapshotPresent" + suffix, false};
        }
        return snapshot.fields(suffix);
    }

    Object[] fields(String suffix) {
        return MiningDiagnosticEmitter.merge(new Object[]{
                        "snapshotPresent" + suffix, true,
                        "snapshotPhase" + suffix, phase,
                        "gameTick" + suffix, tick,
                        "threadName" + suffix, threadName,
                        "pathingBehaviorIdentity" + suffix, pathingBehaviorIdentity,
                        "pathingBehaviorType" + suffix, pathingBehaviorType,
                        "baritonePathing" + suffix, pathing,
                        "safeToCancel" + suffix, safeToCancel,
                        "estimatedTicksToGoal" + suffix, estimatedTicksToGoal,
                        "expectedSegmentStart" + suffix, expectedSegmentStart,
                        "cancelRequested" + suffix, cancelRequested,
                        "calcFailedLastTick" + suffix, calcFailedLastTick,
                        "goalIdentity" + suffix, goalIdentity,
                        "goalType" + suffix, goalType,
                        "goalSummary" + suffix, goalSummary,
                        "inProgressIdentity" + suffix, inProgressIdentity,
                        "inProgressType" + suffix, inProgressType,
                        "inProgressSummary" + suffix, inProgressSummary
                },
                currentExecutor.fields("current", suffix),
                nextExecutor.fields("next", suffix),
                player.fields(suffix),
                target.fields(suffix));
    }

    boolean hasRelevantState() {
        return currentExecutor.present()
                || nextExecutor.present()
                || !"none".equals(inProgressIdentity)
                || !"none".equals(goalIdentity)
                || "true".equals(pathing);
    }

    long tick() {
        return tick;
    }

    String pathingBehaviorIdentity() {
        return pathingBehaviorIdentity;
    }

    String currentExecutorIdentity() {
        return currentExecutor.identity();
    }

    String nextExecutorIdentity() {
        return nextExecutor.identity();
    }

    String goalIdentity() {
        return goalIdentity;
    }

    String inProgressIdentity() {
        return inProgressIdentity;
    }

    String currentExecutorFailed() {
        return currentExecutor.failed();
    }

    String currentExecutorFinished() {
        return currentExecutor.finished();
    }

    String cancelRequested() {
        return cancelRequested;
    }

    String calcFailedLastTick() {
        return calcFailedLastTick;
    }

    int currentExecutorPosition() {
        return currentExecutor.position();
    }

    String currentExecutorPositionText() {
        return currentExecutor.positionText();
    }

    boolean currentExecutorPresent() {
        return currentExecutor.present();
    }

    boolean targetDistanceAvailable() {
        return target.distanceAvailable();
    }

    double targetDistanceSq() {
        return target.distanceSqToPlayerNumeric();
    }

    double playerDisplacementFrom(BaritoneExecutorProgressSnapshot previous) {
        if (previous == null) {
            return UNAVAILABLE_DOUBLE;
        }
        return player.displacementFrom(previous.player);
    }

    String executorPositionDeltaFrom(BaritoneExecutorProgressSnapshot previous) {
        if (previous == null || previous.currentExecutorPosition() == UNAVAILABLE_INT
                || currentExecutorPosition() == UNAVAILABLE_INT) {
            return "unavailable";
        }
        return Integer.toString(currentExecutorPosition() - previous.currentExecutorPosition());
    }

    String targetDistanceDeltaFrom(BaritoneExecutorProgressSnapshot previous) {
        if (previous == null || !previous.targetDistanceAvailable() || !targetDistanceAvailable()) {
            return "unavailable";
        }
        return formatDouble(targetDistanceSq() - previous.targetDistanceSq());
    }

    static String identity(Object value) {
        if (value == null) {
            return "none";
        }
        return Integer.toHexString(System.identityHashCode(value));
    }

    static String className(Object value) {
        return value == null ? "none" : ChatClefDiagnostics.className(value);
    }

    static String safeValue(Supplier<?> supplier) {
        return ChatClefDiagnostics.safeValueForDiagnosticLog(supplier);
    }

    static String formatDouble(double value) {
        if (!Double.isFinite(value)) {
            return "unavailable";
        }
        return String.format("%.5f", value);
    }

    private static String summarizeObject(Object value) {
        if (value == null) {
            return "none";
        }
        return className(value) + "#" + identity(value) + ":" + safeValue(() -> value);
    }

    private static String summarizeFinder(AbstractNodeCostSearch finder) {
        if (finder == null) {
            return "none";
        }
        return className(finder)
                + "#"
                + identity(finder)
                + ",start="
                + safeValue(finder::getStart)
                + ",goal="
                + safeValue(finder::getGoal)
                + ",finished="
                + safeValue(finder::isFinished);
    }

    private static final class ExecutorSnapshot {
        private final boolean present;
        private final String identity;
        private final String type;
        private final int position;
        private final String positionText;
        private final String failed;
        private final String finished;
        private final String pathIdentity;
        private final String pathType;
        private final String pathLength;
        private final String pathMovementCount;
        private final String pathPositionCount;
        private final String pathSrc;
        private final String pathDest;
        private final String pathGoal;

        private ExecutorSnapshot(boolean present,
                                 String identity,
                                 String type,
                                 int position,
                                 String positionText,
                                 String failed,
                                 String finished,
                                 String pathIdentity,
                                 String pathType,
                                 String pathLength,
                                 String pathMovementCount,
                                 String pathPositionCount,
                                 String pathSrc,
                                 String pathDest,
                                 String pathGoal) {
            this.present = present;
            this.identity = identity;
            this.type = type;
            this.position = position;
            this.positionText = positionText;
            this.failed = failed;
            this.finished = finished;
            this.pathIdentity = pathIdentity;
            this.pathType = pathType;
            this.pathLength = pathLength;
            this.pathMovementCount = pathMovementCount;
            this.pathPositionCount = pathPositionCount;
            this.pathSrc = pathSrc;
            this.pathDest = pathDest;
            this.pathGoal = pathGoal;
        }

        static ExecutorSnapshot capture(PathExecutor executor) {
            IPath path = path(executor);
            int position = intValue(() -> executor == null ? UNAVAILABLE_INT : executor.getPosition());
            return new ExecutorSnapshot(
                    executor != null,
                    BaritoneExecutorProgressSnapshot.identity(executor),
                    className(executor),
                    position,
                    position == UNAVAILABLE_INT ? "unavailable" : Integer.toString(position),
                    executor == null ? "none" : safeValue(executor::failed),
                    executor == null ? "none" : safeValue(executor::finished),
                    BaritoneExecutorProgressSnapshot.identity(path),
                    className(path),
                    path == null ? "none" : safeValue(path::length),
                    path == null ? "none" : safeValue(() -> path.movements().size()),
                    path == null ? "none" : safeValue(() -> path.positions().size()),
                    path == null ? "none" : safeValue(path::getSrc),
                    path == null ? "none" : safeValue(path::getDest),
                    path == null ? "none" : safeValue(path::getGoal)
            );
        }

        Object[] fields(String role, String suffix) {
            String prefix = role + "Executor";
            return new Object[]{
                    prefix + "Present" + suffix, present,
                    prefix + "Identity" + suffix, identity,
                    prefix + "Type" + suffix, type,
                    prefix + "Position" + suffix, positionText,
                    prefix + "Failed" + suffix, failed,
                    prefix + "Finished" + suffix, finished,
                    prefix + "PathIdentity" + suffix, pathIdentity,
                    prefix + "PathType" + suffix, pathType,
                    prefix + "PathLength" + suffix, pathLength,
                    prefix + "PathMovementCount" + suffix, pathMovementCount,
                    prefix + "PathPositionCount" + suffix, pathPositionCount,
                    prefix + "PathSrc" + suffix, pathSrc,
                    prefix + "PathDest" + suffix, pathDest,
                    prefix + "PathGoal" + suffix, pathGoal
            };
        }

        boolean present() {
            return present;
        }

        String identity() {
            return identity;
        }

        int position() {
            return position;
        }

        String positionText() {
            return positionText;
        }

        String failed() {
            return failed;
        }

        String finished() {
            return finished;
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

        private static int intValue(Supplier<Integer> supplier) {
            try {
                return supplier.get();
            } catch (RuntimeException | LinkageError error) {
                return UNAVAILABLE_INT;
            }
        }
    }

    private static final class PlayerSnapshot {
        private final boolean present;
        private final String position;
        private final String blockPosition;
        private final String velocity;
        private final String speedSq;
        private final String dimension;
        private final String poseState;
        private final double x;
        private final double y;
        private final double z;

        private PlayerSnapshot(boolean present,
                               String position,
                               String blockPosition,
                               String velocity,
                               String speedSq,
                               String dimension,
                               String poseState,
                               double x,
                               double y,
                               double z) {
            this.present = present;
            this.position = position;
            this.blockPosition = blockPosition;
            this.velocity = velocity;
            this.speedSq = speedSq;
            this.dimension = dimension;
            this.poseState = poseState;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        static PlayerSnapshot capture() {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client == null ? null : client.player;
            Vec3d pos = player == null ? null : safeVec(player::getPos);
            Vec3d velocity = player == null ? null : safeVec(player::getVelocity);
            return new PlayerSnapshot(
                    player != null,
                    player == null ? "none" : safeValue(player::getPos),
                    player == null ? "none" : safeValue(player::getBlockPos),
                    velocity == null ? "none" : ChatClefDiagnostics.vec3d(velocity),
                    velocity == null ? "none" : formatDouble(velocity.lengthSquared()),
                    safeValue(() -> client == null || client.world == null ? "none" : client.world.getRegistryKey().getValue()),
                    player == null ? "none" : poseState(player),
                    pos == null ? UNAVAILABLE_DOUBLE : pos.x,
                    pos == null ? UNAVAILABLE_DOUBLE : pos.y,
                    pos == null ? UNAVAILABLE_DOUBLE : pos.z
            );
        }

        Object[] fields(String suffix) {
            return new Object[]{
                    "playerPresent" + suffix, present,
                    "playerPosition" + suffix, position,
                    "playerBlockPosition" + suffix, blockPosition,
                    "playerVelocity" + suffix, velocity,
                    "playerSpeedSq" + suffix, speedSq,
                    "dimension" + suffix, dimension,
                    "playerPoseState" + suffix, poseState
            };
        }

        double x() {
            return x;
        }

        double y() {
            return y;
        }

        double z() {
            return z;
        }

        double displacementFrom(PlayerSnapshot previous) {
            if (previous == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(previous.x) || !Double.isFinite(previous.y) || !Double.isFinite(previous.z)) {
                return UNAVAILABLE_DOUBLE;
            }
            double dx = x - previous.x;
            double dy = y - previous.y;
            double dz = z - previous.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        private static Vec3d safeVec(Supplier<Vec3d> supplier) {
            try {
                return supplier.get();
            } catch (RuntimeException | LinkageError error) {
                return null;
            }
        }

        private static String poseState(ClientPlayerEntity player) {
            return "onGround=" + safeValue(player::isOnGround)
                    + ",sneaking=" + safeValue(player::isSneaking)
                    + ",sprinting=" + safeValue(player::isSprinting)
                    + ",usingItem=" + safeValue(player::isUsingItem);
        }
    }
}
