package adris.altoclef.tasks.interaction.block;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasks.movement.escape.AnnoyingBlockDetector;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.util.math.BlockPos;

//20260730_kpopmodder: Added this tracker to keep movement/stuck detection out of InteractWithBlockTask orchestration.
final class BlockInteractionProgressTracker {

    private final MovementProgressChecker moveChecker = new MovementProgressChecker();
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final AnnoyingBlockDetector annoyingBlockDetector = new AnnoyingBlockDetector();
    private Task unstuckTask;

    void resetForStart() {
        moveChecker.reset();
        stuckCheck.reset();
    }

    void resetMoveProgress() {
        moveChecker.reset();
    }

    void resetStuckProgress() {
        stuckCheck.reset();
    }

    Task getActiveUnstuckTask(AltoClef mod) {
        if (unstuckTask != null
                && unstuckTask.isActive()
                && !unstuckTask.isFinished()
                && annoyingBlockDetector.findNearbyAnnoyingBlock(mod) != null) {
            return unstuckTask;
        }
        return null;
    }

    StuckRecovery checkStuckOrCreateRecovery(AltoClef mod) {
        if (!moveChecker.check(mod) || !stuckCheck.check(mod)) {
            BlockPos blockStuck = annoyingBlockDetector.findNearbyAnnoyingBlock(mod);
            if (blockStuck != null) {
                unstuckTask = new SafeRandomShimmyTask();
                return new StuckRecovery(blockStuck, unstuckTask);
            }
            stuckCheck.reset();
        }
        return StuckRecovery.none();
    }

    boolean isMovementProgressing(AltoClef mod) {
        return moveChecker.check(mod);
    }

    record StuckRecovery(BlockPos blockStuck, Task task) {
        static StuckRecovery none() {
            return new StuckRecovery(null, null);
        }

        boolean hasTask() {
            return task != null;
        }
    }
}
