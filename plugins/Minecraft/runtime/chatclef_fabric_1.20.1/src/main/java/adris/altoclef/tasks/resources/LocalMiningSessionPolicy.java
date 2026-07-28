package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

//20260728_kpopmodder: Added this policy to keep bulk stone/cobble mining anchored to a local work area.
final class LocalMiningSessionPolicy {
    private static final int SESSION_TICKS = 20 * 45;
    private static final double MAX_ANCHOR_DISTANCE = 14;
    private static final int LOCAL_XZ_RANGE = 5;
    private static final int LOCAL_Y_BELOW = 2;
    private static final int LOCAL_Y_ABOVE = 3;
    private static final Block[] SESSION_BLOCKS = new Block[]{
            Blocks.STONE,
            Blocks.COBBLESTONE,
            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE
    };

    private final boolean enabled;
    private BlockPos anchor;
    private int activeUntilTick;

    LocalMiningSessionPolicy(Block[] blocks) {
        enabled = Arrays.stream(blocks).anyMatch(LocalMiningSessionPolicy::isSessionBlock);
    }

    boolean isEnabled() {
        return enabled;
    }

    void armAfterMiningTarget(BlockPos target, AltoClef mod) {
        if (!enabled || target == null || mod == null || mod.getPlayer() == null) {
            return;
        }
        if (anchor == null || !target.isWithinDistance(WorldHelper.toVec3d(anchor), MAX_ANCHOR_DISTANCE)) {
            anchor = target;
        }
        activeUntilTick = WorldHelper.getTicks() + SESSION_TICKS;
    }

    void armAfterBlockBreak(BlockPos brokenPos) {
        if (!enabled || brokenPos == null) {
            return;
        }
        if (anchor == null || !brokenPos.isWithinDistance(WorldHelper.toVec3d(anchor), MAX_ANCHOR_DISTANCE)) {
            anchor = brokenPos;
        }
        activeUntilTick = WorldHelper.getTicks() + SESSION_TICKS;
    }

    Optional<BlockPos> getPreferredLocalBlock(AltoClef mod, Vec3d playerPos, Predicate<BlockPos> isValid, Block... blocks) {
        pruneExpired(mod);
        if (!enabled || anchor == null || playerPos == null || mod == null || mod.getPlayer() == null) {
            return Optional.empty();
        }
        if (mod.getPlayer().isTouchingWater() || !anchor.isWithinDistance(playerPos, MAX_ANCHOR_DISTANCE)) {
            return Optional.empty();
        }

        BlockPos playerBlock = blockPosFrom(playerPos);
        BlockPos best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (int x = anchor.getX() - LOCAL_XZ_RANGE; x <= anchor.getX() + LOCAL_XZ_RANGE; x++) {
            for (int y = anchor.getY() - LOCAL_Y_BELOW; y <= anchor.getY() + LOCAL_Y_ABOVE; y++) {
                for (int z = anchor.getZ() - LOCAL_XZ_RANGE; z <= anchor.getZ() + LOCAL_XZ_RANGE; z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!isLocalCandidate(mod, playerBlock, candidate, isValid, blocks)) {
                        continue;
                    }

                    double score = score(playerPos, candidate);
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        return Optional.ofNullable(best);
    }

    int ticksRemaining() {
        return Math.max(0, activeUntilTick - WorldHelper.getTicks());
    }

    String describeAnchor() {
        return anchor == null ? "none" : anchor.toShortString();
    }

    void pruneExpired(AltoClef mod) {
        if (anchor == null) {
            return;
        }
        if (WorldHelper.getTicks() > activeUntilTick
                || mod == null
                || mod.getPlayer() == null
                || !anchor.isWithinDistance(mod.getPlayer().getPos(), MAX_ANCHOR_DISTANCE)) {
            reset();
        }
    }

    void reset() {
        anchor = null;
        activeUntilTick = 0;
    }

    private boolean isLocalCandidate(AltoClef mod, BlockPos playerBlock, BlockPos candidate, Predicate<BlockPos> isValid, Block... blocks) {
        if (candidate.equals(playerBlock.down())) {
            return false;
        }
        if (!matchesAnyBlock(mod, candidate, blocks)) {
            return false;
        }
        if (!isValid.test(candidate)) {
            return false;
        }
        return !hasAdjacentFluid(mod, candidate);
    }

    private boolean matchesAnyBlock(AltoClef mod, BlockPos pos, Block... blocks) {
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        for (Block target : blocks) {
            if (block == target) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdjacentFluid(AltoClef mod, BlockPos pos) {
        if (isFluid(mod.getWorld().getBlockState(pos.up()))) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (isFluid(mod.getWorld().getBlockState(pos.offset(direction)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFluid(BlockState state) {
        return state.getBlock() instanceof FluidBlock || !state.getFluidState().isEmpty();
    }

    private double score(Vec3d playerPos, BlockPos candidate) {
        Vec3d candidatePos = WorldHelper.toVec3d(candidate);
        double pathScore = BaritoneHelper.calculateGenericHeuristic(playerPos, candidatePos);
        double anchorScore = anchor == null ? 0 : candidatePos.squaredDistanceTo(WorldHelper.toVec3d(anchor)) * 0.05;
        double yPenalty = Math.abs(candidate.getY() - Math.floor(playerPos.y)) * 1.75;
        if (candidate.getY() < Math.floor(playerPos.y)) {
            yPenalty += 3;
        }
        return pathScore + anchorScore + yPenalty;
    }

    private static boolean isSessionBlock(Block block) {
        for (Block sessionBlock : SESSION_BLOCKS) {
            if (block == sessionBlock) {
                return true;
            }
        }
        return false;
    }

    private BlockPos blockPosFrom(Vec3d pos) {
        return new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
    }
}
