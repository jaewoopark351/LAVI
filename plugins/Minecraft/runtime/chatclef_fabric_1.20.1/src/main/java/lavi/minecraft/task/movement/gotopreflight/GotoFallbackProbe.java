//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight;

import adris.altoclef.AltoClef;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * Deterministic V3 aerial geometry classifier.
 *
 * This class does NOT inspect timeouts, path failures, wander state or retry counts.
 * It only answers: "Is the requested XYZ a loaded, dry, sky-visible air column over
 * a real walkable foundation? No exact native approach waypoint is created."
 */
final class GotoFallbackProbe {

    private static final int MAX_COLUMN_HEIGHT = 384;
    private static final int PREPARATION_RADIUS = 8;
    private static final int BELOW_FOUNDATION_LIMIT = 3;
    private static final int ABOVE_COLUMN_FEET_LIMIT = 1;

    record AirColumn(
            BlockPos foundation,
            BlockPos approach,
            BlockPos target,
            BlockState foundationState
    ) {
        int placements() {
            return target.getY() - approach.getY();
        }
    }

    private GotoFallbackProbe() { }

    static Optional<AirColumn> airColumn(AltoClef mod, BlockPos target) {
        if (!loaded(mod, target)
                || !loaded(mod, target.up())
                || !mod.getWorld().isSkyVisible(target)
                || !mod.getWorld().getBlockState(target).isAir()
                || !mod.getWorld().getBlockState(target.up()).isAir()) {
            return Optional.empty();
        }

        for (int depth = 1; depth <= MAX_COLUMN_HEIGHT; depth++) {
            BlockPos pos = target.down(depth);
            if (!loaded(mod, pos)) return Optional.empty();

            BlockState state = mod.getWorld().getBlockState(pos);
            if (!state.isAir()) {
                // depth == 1 means the goal already stands directly on terrain; no aerial assistance needed.
                if (depth == 1 || !safeFoundation(mod, pos, state)) {
                    return Optional.empty();
                }

                BlockPos approach = pos.up().toImmutable();
                AirColumn result = new AirColumn(
                        pos.toImmutable(),
                        approach,
                        target.toImmutable(),
                        state
                );
                return result.placements() > 0 ? Optional.of(result) : Optional.empty();
            }

            if (mod.getExtraBaritoneSettings().shouldAvoidPlacingAt(pos)) {
                return Optional.empty();
            }

            // Reject water/lava touching the intended pillar cells. The fallback is deliberately dry/simple.
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.offset(direction);
                if (!loaded(mod, neighbor)
                        || !mod.getWorld().getBlockState(neighbor).getFluidState().isEmpty()) {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Null means the player is in the supported local preparation area, not that a
     * path/mining operation is guaranteed to succeed. Never use Y<64 as underground detection.
     */
    static String preparationBlockReason(AltoClef mod, AirColumn column, BlockPos feet) {
        BlockPos f = column.foundation();
        if (Math.abs((long) feet.getX() - f.getX()) > PREPARATION_RADIUS
                || Math.abs((long) feet.getZ() - f.getZ()) > PREPARATION_RADIUS) {
            return "FAR_FROM_FOUNDATION";
        }
        if (feet.getY() < f.getY() - BELOW_FOUNDATION_LIMIT) return "BELOW_PREPARATION_AREA";
        if (feet.getY() > column.approach().getY() + ABOVE_COLUMN_FEET_LIMIT) {
            return "ABOVE_PREPARATION_AREA";
        }
        if (!loaded(mod, feet) || !loaded(mod, feet.up()) || !loaded(mod, feet.down())) {
            return "PREPARATION_AREA_UNLOADED";
        }
        if (!mod.getPlayer().isOnGround() || mod.getPlayer().isTouchingWater()) {
            return "NOT_DRY_GROUNDED";
        }
        if (!mod.getWorld().isSkyVisible(feet)) return "PREPARATION_UNDER_COVER";
        if (!mod.getWorld().getBlockState(feet).isAir()
                || !mod.getWorld().getBlockState(feet.up()).isAir()) {
            return "PREPARATION_BODY_OBSTRUCTED";
        }
        if (!safeFoundation(mod, feet.down(), mod.getWorld().getBlockState(feet.down()))) {
            return "PREPARATION_FOOTING_UNSAFE";
        }
        return null;
    }

    /**
     * A local collection budget, NOT an exact Baritone path placement count.
     * Include remaining vertical height from the LOWER of current feet and column feet,
     * plus horizontal Manhattan displacement to the target column. This avoids pretending
     * the player already reached foundation.up(). Detours can still require more material.
     */
    static int materialBudgetFrom(BlockPos feet, AirColumn column) {
        long startY = Math.min(feet.getY(), column.approach().getY());
        long vertical = Math.max(0L, (long) column.target().getY() - startY);
        long horizontal = Math.abs((long) feet.getX() - column.target().getX())
                + Math.abs((long) feet.getZ() - column.target().getZ());
        long total = vertical + horizontal;
        if (total <= 0 || total > Integer.MAX_VALUE - GotoMaterialPlan.RESERVE) {
            throw new IllegalArgumentException("Invalid local aerial budget: " + total);
        }
        return (int) total;
    }

    static boolean unchanged(AltoClef mod, AirColumn expected) {
        return airColumn(mod, expected.target())
                .filter(expected::equals)
                .isPresent();
    }

    private static boolean loaded(AltoClef mod, BlockPos pos) {
        return pos.getY() >= mod.getWorld().getBottomY()
                && pos.getY() < mod.getWorld().getTopY()
                && mod.getWorld().isChunkLoaded(pos)
                && mod.getWorld().getWorldBorder().contains(pos);
    }

    private static boolean safeFoundation(AltoClef mod, BlockPos pos, BlockState state) {
        return state.getFluidState().isEmpty()
                && mod.getWorld().getBlockEntity(pos) == null
                && !(state.getBlock() instanceof FallingBlock)
                && !state.isOf(Blocks.MAGMA_BLOCK)
                && !state.isOf(Blocks.CACTUS)
                && !state.isOf(Blocks.SLIME_BLOCK)
                && !state.isOf(Blocks.HONEY_BLOCK)
                && state.isSideSolidFullSquare(mod.getWorld(), pos, Direction.UP)
                && MovementHelper.canWalkOn(
                    mod.getClientBaritone().bsi,
                    pos.getX(), pos.getY(), pos.getZ());
    }
}
//#endif
