//#if MC == 12001
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import baritone.api.process.IExploreProcess;
import net.minecraft.util.math.BlockPos;

/**
 * Reuses TimeoutWanderTask's actual exploration primitive. Deliberately excludes its
 * kill-annoying-mob, shimmy/left-click, cursor-inventory, and global forceCancel branches.
 */
final class FindExploreTask extends FindMovementTask {
    private IExploreProcess process;
    private BlockPos anchor;
    private boolean ownsProcess;

    FindExploreTask(FindTask owner) { super(owner); }

    @Override protected void prepareMovement() {
        var mod = AltoClef.getInstance();
        anchor = mod.getPlayer().getBlockPos().toImmutable();
        process = mod.getClientBaritone().getExploreProcess();
        ownsProcess = false;
    }

    @Override protected void tickMovement() {
        if (!process.isActive()) {
            // Mark before the call so an exceptional partial start is still cleaned up locally.
            acquireMovementPolicy();
            ownsProcess = true;
            process.explore(anchor.getX(), anchor.getZ());
            owner.boundary("explore_started", "anchor", anchor.toShortString());
        } else if (!ownsProcess) {
            owner.boundary("explore_busy", "action", "wait_without_stealing_process");
        }
    }

    @Override protected void releaseMovement() {
        boolean owned = ownsProcess;
        ownsProcess = false;
        if (owned && process != null) {
            process.onLostControl();
            owner.boundary("explore_released", "anchor", anchor.toShortString());
        }
        // Never cancel custom-goal/defense processes or release CLICK_LEFT/CLICK_RIGHT here.
    }

    @Override public boolean isFinished() { return false; }
    @Override protected String toDebugString() { return "FIND exploring with native Baritone"; }
}
//#endif
