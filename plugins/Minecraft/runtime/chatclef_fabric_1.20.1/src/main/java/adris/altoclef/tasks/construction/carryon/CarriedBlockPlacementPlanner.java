package adris.altoclef.tasks.construction.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.IPlayerContext;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

//20260728_kpopmodder: Added this planner to keep Carry On placement target selection separate from task execution.
public final class CarriedBlockPlacementPlanner {

    private static final int SEARCH_RANGE = 4;

    public Optional<PlacementTarget> findNearest(AltoClef mod, BlockState carriedState) {
        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        BlockPos playerPos = ctx.playerFeet();
        PlacementTarget best = null;

        for (int x = -SEARCH_RANGE; x <= SEARCH_RANGE; ++x) {
            for (int y = -1; y <= 2; ++y) {
                for (int z = -SEARCH_RANGE; z <= SEARCH_RANGE; ++z) {
                    BlockPos placePos = playerPos.add(x, y, z);
                    Optional<PlacementTarget> candidate = findBestSupport(mod, carriedState, placePos);
                    if (candidate.isEmpty()) {
                        continue;
                    }
                    PlacementTarget target = candidate.get();
                    if (best == null || target.score() < best.score()) {
                        best = target;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public boolean isValid(AltoClef mod, PlacementTarget target, BlockState carriedState) {
        if (target == null) {
            return false;
        }
        if (!canPlaceAt(mod, carriedState, target.placePos())) {
            return false;
        }
        return canUseSupport(mod, target.supportPos());
    }

    private Optional<PlacementTarget> findBestSupport(AltoClef mod, BlockState carriedState, BlockPos placePos) {
        if (!canPlaceAt(mod, carriedState, placePos)) {
            return Optional.empty();
        }

        PlacementTarget best = null;
        for (Direction face : Direction.values()) {
            BlockPos supportPos = placePos.offset(face.getOpposite());
            if (!canUseSupport(mod, supportPos)) {
                continue;
            }
            double score = score(mod, placePos, face);
            PlacementTarget target = new PlacementTarget(placePos, supportPos, face, score);
            if (best == null || target.score() < best.score()) {
                best = target;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean canPlaceAt(AltoClef mod, BlockState carriedState, BlockPos placePos) {
        if (!WorldHelper.canPlace(placePos)) {
            return false;
        }
        if (WorldHelper.isInsidePlayer(placePos)) {
            return false;
        }
        return mod.getWorld().canPlace(carriedState, placePos, ShapeContext.absent());
    }

    private boolean canUseSupport(AltoClef mod, BlockPos supportPos) {
        if (!WorldHelper.canReach(supportPos)) {
            return false;
        }
        if (WorldHelper.isInteractableBlock(supportPos)) {
            return false;
        }
        return MovementHelper.canPlaceAgainst(mod.getClientBaritone().getPlayerContext(), supportPos);
    }

    private double score(AltoClef mod, BlockPos placePos, Direction face) {
        double distance = BlockPosVer.getSquaredDistance(placePos, mod.getPlayer().getPos());
        double floorPenalty = MovementHelper.canPlaceAgainst(mod.getClientBaritone().getPlayerContext(), placePos.down()) ? 0 : 3;
        double sidePenalty = face == Direction.UP ? 0 : 1.5;
        return distance + floorPenalty + sidePenalty;
    }

    public record PlacementTarget(BlockPos placePos, BlockPos supportPos, Direction supportFace, double score) {
    }
}
