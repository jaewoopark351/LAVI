package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Finds the closest reachable block and runs a task on that block.
 */
public class DoToClosestBlockTask extends AbstractDoToClosestObjectTask<BlockPos> {

    private final Block[] targetBlocks;

    private final Supplier<Vec3d> getOriginPos;
    private final Function<Vec3d, Optional<BlockPos>> getClosest;

    private final Function<BlockPos, Task> getTargetTask;

    private final Predicate<BlockPos> isValid;

    public DoToClosestBlockTask(Supplier<Vec3d> getOriginSupplier, Function<BlockPos, Task> getTargetTask, Function<Vec3d, Optional<BlockPos>> getClosestBlock, Predicate<BlockPos> isValid, Block... blocks) {
        getOriginPos = getOriginSupplier;
        this.getTargetTask = getTargetTask;
        getClosest = getClosestBlock;
        this.isValid = isValid;
        targetBlocks = blocks;
    }

    public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Function<Vec3d, Optional<BlockPos>> getClosestBlock, Predicate<BlockPos> isValid, Block... blocks) {
        this(null, getTargetTask, getClosestBlock, isValid, blocks);
    }

    public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Predicate<BlockPos> isValid, Block... blocks) {
        this(null, getTargetTask, null, isValid, blocks);
    }

    public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Block... blocks) {
        this(getTargetTask, null, blockPos -> true, blocks);
    }

    @Override
    protected Vec3d getPos(AltoClef mod, BlockPos obj) {
        return WorldHelper.toVec3d(obj);
    }

    @Override
    protected Optional<BlockPos> getClosestTo(AltoClef mod, Vec3d pos) {
        Optional<BlockPos> result;
        if (getClosest != null) {
            result = getClosest.apply(pos);
        } else {
            result = mod.getBlockScanner().getNearestBlock(pos, isValid, targetBlocks);
        }
        ChatClefDiagnostics.logEvent("CLOSEST_BLOCK", "GET_CLOSEST", "closest_block_result", this,
                "origin", pos,
                "targetBlocks", Arrays.toString(targetBlocks),
                "resultPresent", result.isPresent(),
                "result", result.map(Object::toString).orElse("none"),
                "resultBlockState", result.map(blockPos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(blockPos))).orElse("unavailable"));
        StoreDepositDiagnostics.logFilteredSearchResult(this, result, targetBlocks);
        return result;
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        if (getOriginPos != null) {
            return getOriginPos.get();
        }
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(BlockPos obj) {
        Task task = getTargetTask.apply(obj);
        ChatClefDiagnostics.logTaskTransition(this, null, task, "closest_block_goal_task_created",
                "targetBlocks", Arrays.toString(targetBlocks),
                "goalBlockPosition", obj,
                "goalBlockState", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getWorld().getBlockState(obj)));
        return task;
    }

    @Override
    protected boolean isValid(AltoClef mod, BlockPos obj) {
        // Assume we're valid since we're in the same chunk.
        boolean chunkLoaded = mod.getChunkTracker().isChunkLoaded(obj);
        if (!chunkLoaded) {
            ChatClefDiagnostics.logEvent("CLOSEST_BLOCK", "VALIDATE", "chunk_unloaded_assume_valid", this,
                    "targetBlocks", Arrays.toString(targetBlocks),
                    "blockPosition", obj);
            return true;
        }
        // Our valid predicate
        boolean predicateValid = isValid == null || isValid.test(obj);
        if (!predicateValid) {
            StoreDepositDiagnostics.logPursuitDecision(this, obj, obj, "PURSUIT_VALIDATION_REJECTED_BY_PREDICATE");
            ChatClefDiagnostics.logEvent("CLOSEST_BLOCK", "VALIDATE", "predicate_rejected_block", this,
                    "targetBlocks", Arrays.toString(targetBlocks),
                    "blockPosition", obj,
                    "blockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(obj)));
            return false;
        }
        // Correct block
        boolean blockMatches = mod.getBlockScanner().isBlockAtPosition(obj, targetBlocks);
        StoreDepositDiagnostics.logPursuitDecision(this, obj, obj, blockMatches ? "PURSUIT_VALIDATION_ACCEPTED" : "PURSUIT_VALIDATION_BLOCK_TYPE_MISMATCH");
        ChatClefDiagnostics.logEvent("CLOSEST_BLOCK", "VALIDATE", "block_match_check", this,
                "targetBlocks", Arrays.toString(targetBlocks),
                "blockPosition", obj,
                "blockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(obj)),
                "blockMatches", blockMatches);
        return blockMatches;
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoToClosestBlockTask task) {
            return Arrays.equals(task.targetBlocks, targetBlocks);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing something to closest block...";
    }
}
