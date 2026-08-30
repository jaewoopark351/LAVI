package lavi.minecraft.diagnostics.mining.baritone.executor;

import adris.altoclef.util.baritone.GoalFollowEntity;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;

//20260830_kpopmodder: Observe only target distance needed for cheap semantic progress classification.
final class BaritoneExecutorTargetObservation {
    private static final Field GOAL_ENTITY_FIELD = goalEntityField();

    private BaritoneExecutorTargetObservation() {
    }

    static double distanceSq(Goal goal, double playerX, double playerY, double playerZ) {
        if (!(goal instanceof GoalFollowEntity)
                || GOAL_ENTITY_FIELD == null
                || !Double.isFinite(playerX)
                || !Double.isFinite(playerY)
                || !Double.isFinite(playerZ)) {
            return Double.NaN;
        }
        try {
            Object value = GOAL_ENTITY_FIELD.get(goal);
            if (!(value instanceof Entity)) {
                return Double.NaN;
            }
            Vec3d target = ((Entity) value).getPos();
            if (target == null) {
                return Double.NaN;
            }
            double dx = target.x - playerX;
            double dy = target.y - playerY;
            double dz = target.z - playerZ;
            return dx * dx + dy * dy + dz * dz;
        } catch (IllegalAccessException | RuntimeException | LinkageError error) {
            return Double.NaN;
        }
    }

    private static Field goalEntityField() {
        try {
            Field field = GoalFollowEntity.class.getDeclaredField("entity");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | RuntimeException | LinkageError error) {
            return null;
        }
    }
}
