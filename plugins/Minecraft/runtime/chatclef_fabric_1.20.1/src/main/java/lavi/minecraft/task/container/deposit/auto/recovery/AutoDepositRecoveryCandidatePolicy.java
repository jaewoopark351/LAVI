package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public final class AutoDepositRecoveryCandidatePolicy {
    public static final double MAX_DISTANCE = 50.0;

    public boolean accepts(AltoClef mod, BlockPos position) {
        if (mod.getPlayer() == null
                || !position.isWithinDistance(mod.getPlayer().getPos(), MAX_DISTANCE)
                || mod.getBlockScanner().isUnreachable(position)) {
            return false;
        }
        if (!mod.getChunkTracker().isChunkLoaded(position)) {
            return true;
        }
        Block block = mod.getWorld().getBlockState(position).getBlock();
        if (Arrays.stream(StoreInContainerTask.CONTAINER_BLOCKS).noneMatch(block::equals)) {
            return false;
        }
        if (WorldHelper.isChest(position)
                && WorldHelper.isSolidBlock(position.up())
                && !WorldHelper.canBreak(position.up())) {
            return false;
        }
        return !isAvoidedDungeonChest(mod, position);
    }

    private static boolean isAvoidedDungeonChest(AltoClef mod, BlockPos position) {
        if (!WorldHelper.isChest(position)
                || !mod.getModSettings().shouldAvoidSearchingForDungeonChests()) {
            return false;
        }
        int range = 6;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                if (mod.getWorld().getBlockState(position.add(dx, 0, dz)).getBlock() == Blocks.SPAWNER) {
                    return true;
                }
            }
        }
        return false;
    }
}
