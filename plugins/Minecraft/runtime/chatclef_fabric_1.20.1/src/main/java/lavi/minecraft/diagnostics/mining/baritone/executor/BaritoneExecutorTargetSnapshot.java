package lavi.minecraft.diagnostics.mining.baritone.executor;

import adris.altoclef.util.baritone.GoalFollowEntity;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;

final class BaritoneExecutorTargetSnapshot {
    private static final Field GOAL_ENTITY_FIELD = goalField("entity");
    private static final Field GOAL_CLOSE_ENOUGH_FIELD = goalField("closeEnoughDistance");

    private final boolean available;
    private final String reason;
    private final String goalIdentity;
    private final String goalType;
    private final String targetIdentity;
    private final String targetType;
    private final String targetUuid;
    private final String targetEntityId;
    private final String targetPosition;
    private final String targetBlockPosition;
    private final String targetAlive;
    private final String targetRemoved;
    private final String closeEnoughDistance;
    private final String distanceSqToPlayer;
    private final String horizontalDistanceSqToPlayer;
    private final double distanceSqToPlayerNumeric;

    private BaritoneExecutorTargetSnapshot(boolean available,
                                           String reason,
                                           String goalIdentity,
                                           String goalType,
                                           String targetIdentity,
                                           String targetType,
                                           String targetUuid,
                                           String targetEntityId,
                                           String targetPosition,
                                           String targetBlockPosition,
                                           String targetAlive,
                                           String targetRemoved,
                                           String closeEnoughDistance,
                                           String distanceSqToPlayer,
                                           String horizontalDistanceSqToPlayer,
                                           double distanceSqToPlayerNumeric) {
        this.available = available;
        this.reason = reason;
        this.goalIdentity = goalIdentity;
        this.goalType = goalType;
        this.targetIdentity = targetIdentity;
        this.targetType = targetType;
        this.targetUuid = targetUuid;
        this.targetEntityId = targetEntityId;
        this.targetPosition = targetPosition;
        this.targetBlockPosition = targetBlockPosition;
        this.targetAlive = targetAlive;
        this.targetRemoved = targetRemoved;
        this.closeEnoughDistance = closeEnoughDistance;
        this.distanceSqToPlayer = distanceSqToPlayer;
        this.horizontalDistanceSqToPlayer = horizontalDistanceSqToPlayer;
        this.distanceSqToPlayerNumeric = distanceSqToPlayerNumeric;
    }

    static BaritoneExecutorTargetSnapshot capture(Goal goal, double playerX, double playerY, double playerZ) {
        if (!(goal instanceof GoalFollowEntity)) {
            return unavailable(goal, goal == null ? "goal_absent" : "goal_not_goal_follow_entity");
        }
        if (GOAL_ENTITY_FIELD == null) {
            return unavailable(goal, "goal_follow_entity_field_missing");
        }
        Entity entity;
        try {
            Object rawEntity = GOAL_ENTITY_FIELD.get(goal);
            if (!(rawEntity instanceof Entity)) {
                return unavailable(goal, "goal_follow_entity_target_not_entity");
            }
            entity = (Entity) rawEntity;
        } catch (IllegalAccessException | RuntimeException | LinkageError error) {
            return unavailable(goal, "goal_follow_entity_target_read_failed:" + error.getClass().getSimpleName());
        }
        Vec3d targetPos = safeVec(entity);
        double distanceSq = distanceSq(targetPos, playerX, playerY, playerZ);
        double horizontalDistanceSq = horizontalDistanceSq(targetPos, playerX, playerZ);
        return new BaritoneExecutorTargetSnapshot(
                true,
                "target_observed_from_goal_follow_entity",
                BaritoneExecutorProgressSnapshot.identity(goal),
                BaritoneExecutorProgressSnapshot.className(goal),
                BaritoneExecutorProgressSnapshot.identity(entity),
                BaritoneExecutorProgressSnapshot.safeValue(entity::getType),
                BaritoneExecutorProgressSnapshot.safeValue(entity::getUuidAsString),
                BaritoneExecutorProgressSnapshot.safeValue(entity::getId),
                targetPos == null ? "unavailable" : BaritoneExecutorProgressSnapshot.safeValue(entity::getPos),
                BaritoneExecutorProgressSnapshot.safeValue(entity::getBlockPos),
                BaritoneExecutorProgressSnapshot.safeValue(entity::isAlive),
                BaritoneExecutorProgressSnapshot.safeValue(entity::isRemoved),
                closeEnoughDistance(goal),
                BaritoneExecutorProgressSnapshot.formatDouble(distanceSq),
                BaritoneExecutorProgressSnapshot.formatDouble(horizontalDistanceSq),
                distanceSq
        );
    }

    Object[] fields(String suffix) {
        return new Object[]{
                "targetSnapshotAvailable" + suffix, available,
                "targetSnapshotReason" + suffix, reason,
                "targetGoalIdentity" + suffix, goalIdentity,
                "targetGoalType" + suffix, goalType,
                "targetEntityIdentity" + suffix, targetIdentity,
                "targetEntityType" + suffix, targetType,
                "targetUuid" + suffix, targetUuid,
                "targetEntityId" + suffix, targetEntityId,
                "targetPosition" + suffix, targetPosition,
                "targetBlockPosition" + suffix, targetBlockPosition,
                "targetAlive" + suffix, targetAlive,
                "targetRemoved" + suffix, targetRemoved,
                "targetCloseEnoughDistance" + suffix, closeEnoughDistance,
                "targetDistanceSqToPlayer" + suffix, distanceSqToPlayer,
                "targetHorizontalDistanceSqToPlayer" + suffix, horizontalDistanceSqToPlayer
        };
    }

    boolean distanceAvailable() {
        return Double.isFinite(distanceSqToPlayerNumeric);
    }

    double distanceSqToPlayerNumeric() {
        return distanceSqToPlayerNumeric;
    }

    private static BaritoneExecutorTargetSnapshot unavailable(Goal goal, String reason) {
        return new BaritoneExecutorTargetSnapshot(
                false,
                reason,
                BaritoneExecutorProgressSnapshot.identity(goal),
                BaritoneExecutorProgressSnapshot.className(goal),
                "none",
                "none",
                "none",
                "none",
                "none",
                "none",
                "none",
                "none",
                "none",
                "unavailable",
                "unavailable",
                Double.NaN
        );
    }

    private static Field goalField(String name) {
        try {
            Field field = GoalFollowEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static String closeEnoughDistance(Goal goal) {
        if (GOAL_CLOSE_ENOUGH_FIELD == null) {
            return "unavailable_field_missing";
        }
        try {
            Object value = GOAL_CLOSE_ENOUGH_FIELD.get(goal);
            return String.valueOf(value);
        } catch (IllegalAccessException | RuntimeException | LinkageError error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }

    private static Vec3d safeVec(Entity entity) {
        try {
            return entity.getPos();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static double distanceSq(Vec3d targetPos, double playerX, double playerY, double playerZ) {
        if (targetPos == null
                || !Double.isFinite(playerX)
                || !Double.isFinite(playerY)
                || !Double.isFinite(playerZ)) {
            return Double.NaN;
        }
        double dx = targetPos.x - playerX;
        double dy = targetPos.y - playerY;
        double dz = targetPos.z - playerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double horizontalDistanceSq(Vec3d targetPos, double playerX, double playerZ) {
        if (targetPos == null || !Double.isFinite(playerX) || !Double.isFinite(playerZ)) {
            return Double.NaN;
        }
        double dx = targetPos.x - playerX;
        double dz = targetPos.z - playerZ;
        return dx * dx + dz * dz;
    }
}
