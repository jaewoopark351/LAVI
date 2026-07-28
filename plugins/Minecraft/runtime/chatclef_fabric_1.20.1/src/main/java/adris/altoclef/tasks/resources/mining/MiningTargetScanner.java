package adris.altoclef.tasks.resources.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Predicate;

//20260728_kpopmodder: Added this scanner to keep mining/drop candidate lookup separate from task orchestration.
final class MiningTargetScanner {
    private MiningTargetScanner() {
    }

    static Pair<Double, Optional<ItemEntity>> getClosestItemDrop(AltoClef mod, Vec3d pos, ItemTarget... items) {
        Optional<ItemEntity> closestDrop = Optional.empty();
        if (mod.getEntityTracker().itemDropped(items)) {
            closestDrop = mod.getEntityTracker().getClosestItemDrop(pos, items);
        }

        return new Pair<>(
                // + 5 to make the bot stop mining a bit less
                closestDrop.map(itemEntity -> itemEntity.squaredDistanceTo(pos) + 10).orElse(Double.POSITIVE_INFINITY),
                closestDrop
        );
    }

    static Pair<Double, Optional<BlockPos>> getClosestBlock(AltoClef mod, Vec3d pos, Block... blocks) {
        return getClosestBlock(mod, pos, check -> true, blocks);
    }

    static Pair<Double, Optional<BlockPos>> getClosestBlock(AltoClef mod, Vec3d pos, Predicate<BlockPos> isValidTest, Block... blocks) {
        Optional<BlockPos> closestBlock = mod.getBlockScanner().getNearestBlock(pos, check -> {
            if (mod.getBlockScanner().isUnreachable(check)) return false;
            if (!isValidTest.test(check)) return false;
            return WorldHelper.canBreak(check);
        }, blocks);

        return new Pair<>(
                closestBlock.map(blockPos -> BlockPosVer.getSquaredDistance(blockPos, pos)).orElse(Double.POSITIVE_INFINITY),
                closestBlock
        );
    }

    static boolean isUsableDrop(ItemEntity drop) {
        return drop != null && drop.isAlive() && !drop.getStack().isEmpty();
    }
}
