package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.MagmaBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.util.math.BlockPos;

import java.util.List;

//20260729_kpopmodder: Added this policy to keep block safety decisions separate from terrain escape execution.
public class EscapeBlockActionPolicy {
    private static final int MAX_BLOCKS_TO_CLEAR = 8;

    boolean appendClearBlock(AltoClef mod, BlockPos pos, List<BlockPos> blocksToClear) {
        if (isEscapeSpaceClear(mod, pos)) {
            return true;
        }
        if (blocksToClear.size() >= MAX_BLOCKS_TO_CLEAR || !isSafeBreakTarget(mod, pos)) {
            return false;
        }
        if (!blocksToClear.contains(pos)) {
            blocksToClear.add(pos);
        }
        return true;
    }

    boolean isEscapeSpaceClear(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.getBlock() instanceof FluidBlock) {
            return false;
        }
        return state.getCollisionShape(mod.getWorld(), pos).isEmpty();
    }

    boolean hasSafeFloor(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        return !state.isAir()
                && !(block instanceof FluidBlock)
                && !(block instanceof CactusBlock)
                && !(block instanceof CampfireBlock)
                && !(block instanceof MagmaBlock)
                && !(block instanceof AbstractFireBlock)
                && state.isSolidBlock(mod.getWorld(), pos);
    }

    boolean isSafeBreakTarget(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (isProtectedBlock(pos, block) || !WorldHelper.canBreak(pos)) {
            return false;
        }
        return !(block instanceof FallingBlock) || WorldHelper.fallingBlockSafeToBreak(pos);
    }

    String describeEscapeSpace(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()) {
            return "clear air";
        }
        if (block instanceof FluidBlock) {
            return "blocked by fluid block=" + block.getTranslationKey();
        }
        if (state.getCollisionShape(mod.getWorld(), pos).isEmpty()) {
            return "clear non-colliding block=" + block.getTranslationKey();
        }
        return "blocked by solid collision block=" + block.getTranslationKey();
    }

    String describeFloorSafety(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()) {
            return "unsafe floor: air";
        }
        if (block instanceof FluidBlock) {
            return "unsafe floor: fluid block=" + block.getTranslationKey();
        }
        if (block instanceof CactusBlock
                || block instanceof CampfireBlock
                || block instanceof MagmaBlock
                || block instanceof AbstractFireBlock) {
            return "unsafe floor: damaging block=" + block.getTranslationKey();
        }
        if (!state.isSolidBlock(mod.getWorld(), pos)) {
            return "unsafe floor: not solid block=" + block.getTranslationKey();
        }
        return "safe floor: block=" + block.getTranslationKey();
    }

    String describeBreakSafety(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (isProtectedBlock(pos, block)) {
            return "unsafe break: protected block=" + block.getTranslationKey();
        }
        if (!WorldHelper.canBreak(pos)) {
            return "unsafe break: WorldHelper.canBreak=false block=" + block.getTranslationKey();
        }
        if (block instanceof FallingBlock && !WorldHelper.fallingBlockSafeToBreak(pos)) {
            return "unsafe break: falling block not safe block=" + block.getTranslationKey();
        }
        return "safe break: block=" + block.getTranslationKey();
    }

    private boolean isProtectedBlock(BlockPos pos, Block block) {
        return WorldHelper.isInteractableBlock(pos)
                || block instanceof BedBlock
                || block instanceof SpawnerBlock
                || block instanceof NetherPortalBlock
                || block instanceof EndPortalBlock
                || block instanceof EndPortalFrameBlock
                || block instanceof FluidBlock;
    }
}
