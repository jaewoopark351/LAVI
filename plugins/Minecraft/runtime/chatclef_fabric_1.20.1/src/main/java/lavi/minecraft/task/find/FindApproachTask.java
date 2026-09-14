//#if MC == 12001
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.util.baritone.GoalFollowEntity;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/** Movement only. Combat remains owned by the existing MobDefenseChain and KillAura. */
final class FindApproachTask extends FindMovementTask {
    private final Goal goal;
    private final Entity entity;
    private ICustomGoalProcess process;
    private BlockPos lastPlannedTarget;
    private boolean ownsGoal;

    FindApproachTask(FindTask owner, Entity entity) {
        super(owner);
        this.entity = entity;
        goal = new GoalFollowEntity(entity, 3.0);
    }

    FindApproachTask(FindTask owner, BlockPos stand) {
        super(owner);
        entity = null;
        goal = new GoalBlock(stand);
    }

    @Override protected void prepareMovement() {
        process = AltoClef.getInstance().getClientBaritone().getCustomGoalProcess();
        ownsGoal = false;
        lastPlannedTarget = null;
    }

    @Override protected void tickMovement() {
        if (process.isActive() && (!ownsGoal || process.getGoal() != goal)) {
            releaseSafely();
            owner.boundary("approach_busy", "action", "wait_without_stealing_goal");
            return;
        }
        boolean moved = entity != null && lastPlannedTarget != null
                && entity.getBlockPos().getSquaredDistance(lastPlannedTarget) > 4.0;
        if ((!process.isActive() || moved)
                && AltoClef.getInstance().getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            acquireMovementPolicy();
            ownsGoal = true;
            process.setGoalAndPath(goal);
            if (entity != null) lastPlannedTarget = entity.getBlockPos().toImmutable();
            owner.boundary("approach_goal", "movingTargetReplan", moved,
                    "targetPosition", lastPlannedTarget == null ? "block_stand" : lastPlannedTarget.toShortString());
        }
    }

    @Override protected void releaseMovement() {
        boolean owned = ownsGoal;
        ownsGoal = false;
        if (owned && process != null && process.getGoal() == goal) {
            process.onLostControl();
            owner.boundary("approach_released", "goalIdentityMatched", true);
        }
        // An intervening owner's different goal, combat input, and explore process are untouched.
    }

    @Override public boolean isFinished() {
        var player = AltoClef.getInstance().getPlayer();
        return player != null && goal.isInGoal(player.getBlockPos());
    }
    @Override protected String toDebugString() { return "FIND movement only"; }
}
//#endif
