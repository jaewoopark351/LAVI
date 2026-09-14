//#if MC == 12001
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.BotBehaviour;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** The active movement leaf owns its policy. No parent pop can outlive or corrupt a defense task. */
abstract class FindMovementTask extends Task {
    protected final FindTask owner;
    private BotBehaviour policyOwner;

    FindMovementTask(FindTask owner) { this.owner = owner; }

    @Override protected final void onStart() {
        try {
            prepareMovement();
            owner.boundary("movement_start", "movementClass", getClass().getSimpleName());
        } catch (RuntimeException error) {
            // Task.onStart failures otherwise bypass onStop. Unwind our own lease here as well.
            releaseSafely();
            owner.movementFailed(error);
        }
    }

    @Override protected final Task onTick() {
        if (owner.isFinished()) return null;
        try {
            tickMovement();
        } catch (RuntimeException error) {
            releaseSafely();
            owner.movementFailed(error);
        }
        return null;
    }

    @Override protected final void onStop(Task interruptTask) {
        releaseSafely();
        owner.boundary("movement_released", "movementClass", getClass().getSimpleName(),
                "interruptTask", interruptTask == null ? "none" : interruptTask.getClass().getSimpleName());
    }

    protected final void acquireMovementPolicy() {
        if (policyOwner != null) return;
        BlockPos protectedBlock = owner.protectedBlock();
        Entity droppedItem = owner.droppedItemTarget();
        if (protectedBlock == null && droppedItem == null) return;
        var behaviour = AltoClef.getInstance().getBehaviour();
        behaviour.push();
        policyOwner = behaviour;
        // Preserve native movement permissions. Only the FIND target itself is protected.
        if (protectedBlock != null) {
            behaviour.avoidBlockBreaking(protectedBlock::equals);
            behaviour.avoidBlockPlacing(protectedBlock::equals);
        }
        if (droppedItem != null) {
            behaviour.avoidWalkingThrough(pos -> droppedItem.isAlive() && !droppedItem.isRemoved()
                    && Vec3d.ofBottomCenter(pos).squaredDistanceTo(droppedItem.getPos()) < 4.0);
        }
        owner.boundary("movement_policy_acquired", "targetBlockProtected", protectedBlock != null,
                "droppedItemStandOff", droppedItem != null);
    }

    protected final void releaseSafely() {
        try {
            releaseMovement();
        } catch (RuntimeException error) {
            owner.boundary("movement_release_error", "exception", error.getClass().getSimpleName());
            owner.movementFailed(error);
        } finally {
            BotBehaviour captured = policyOwner;
            policyOwner = null;
            if (captured != null) {
                try {
                    captured.pop();
                } catch (RuntimeException error) {
                    owner.boundary("policy_release_error", "exception", error.getClass().getSimpleName());
                    owner.movementFailed(error);
                }
            }
        }
    }

    protected abstract void prepareMovement();
    protected abstract void tickMovement();
    protected abstract void releaseMovement();
    @Override protected final boolean isEqual(Task other) { return this == other; }
}
//#endif
