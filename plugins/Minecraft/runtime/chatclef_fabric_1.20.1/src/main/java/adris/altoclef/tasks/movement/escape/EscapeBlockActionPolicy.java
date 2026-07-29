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
import java.util.Optional;

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

    //20260729_kpopmodder: This only describes a possible support block; escape execution still never places blocks.
    Optional<EscapePlaceCandidate> planSupportPlaceCandidate(AltoClef mod, BlockPos target, String reason) {
        BlockState targetState = mod.getWorld().getBlockState(target);
        BlockPos support = target.down();
        if (!targetState.isAir() || !hasSafeFloor(mod, support)) {
            return Optional.empty();
        }
        return Optional.of(new EscapePlaceCandidate(target, support, reason));
    }

    String describeSupportPlaceCandidate(AltoClef mod, BlockPos target, String reason) {
        return planSupportPlaceCandidate(mod, target, reason)
                .map(EscapePlaceCandidate::describe)
                .orElse("none");
    }

    boolean isSafeBreakTarget(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (isProtectedBlock(pos, block) || !WorldHelper.canBreak(pos)) {
            return false;
        }
        return !(block instanceof FallingBlock) || WorldHelper.fallingBlockSafeToBreak(pos);
    }

    //20260729_kpopmodder: Keep the next clear-block decision separate from LocalTerrainEscapeTask action execution.
    ClearanceDecision planNextClearance(AltoClef mod, EscapePlan plan, int startIndex) {
        int clearIndex = firstBlockedClearIndex(mod, plan, startIndex);
        if (plan.isClearComplete(clearIndex)) {
            return ClearanceDecision.routeCleared(clearIndex);
        }

        BlockPos target = plan.getBlockToClear(clearIndex);
        if (!isSafeBreakTarget(mod, target)) {
            return ClearanceDecision.targetUnsafe(clearIndex, target);
        }
        return ClearanceDecision.clearTarget(clearIndex, target);
    }

    private int firstBlockedClearIndex(AltoClef mod, EscapePlan plan, int startIndex) {
        int clearIndex = startIndex;
        while (!plan.isClearComplete(clearIndex)
                && isEscapeSpaceClear(mod, plan.getBlockToClear(clearIndex))) {
            clearIndex++;
        }
        return clearIndex;
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

    static final class ClearanceDecision {
        private final ClearanceStatus status;
        private final int clearIndex;
        private final BlockPos target;

        private ClearanceDecision(ClearanceStatus status, int clearIndex, BlockPos target) {
            this.status = status;
            this.clearIndex = clearIndex;
            this.target = target;
        }

        static ClearanceDecision clearTarget(int clearIndex, BlockPos target) {
            return new ClearanceDecision(ClearanceStatus.CLEAR_TARGET, clearIndex, target);
        }

        static ClearanceDecision routeCleared(int clearIndex) {
            return new ClearanceDecision(ClearanceStatus.ROUTE_CLEARED, clearIndex, null);
        }

        static ClearanceDecision targetUnsafe(int clearIndex, BlockPos target) {
            return new ClearanceDecision(ClearanceStatus.TARGET_UNSAFE, clearIndex, target);
        }

        int getClearIndex() {
            return clearIndex;
        }

        BlockPos getTarget() {
            return target;
        }

        boolean isRouteCleared() {
            return status == ClearanceStatus.ROUTE_CLEARED;
        }

        boolean isTargetUnsafe() {
            return status == ClearanceStatus.TARGET_UNSAFE;
        }
    }

    private enum ClearanceStatus {
        CLEAR_TARGET,
        ROUTE_CLEARED,
        TARGET_UNSAFE
    }
}
