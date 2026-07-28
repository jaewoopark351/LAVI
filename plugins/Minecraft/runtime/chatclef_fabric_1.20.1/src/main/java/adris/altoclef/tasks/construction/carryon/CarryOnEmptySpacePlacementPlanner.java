package adris.altoclef.tasks.construction.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.IPlayerContext;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

//20260728_kpopmodder: Added this planner to find Carry On placement spots that are block-empty even when nearby entities crowd the area.
public final class CarryOnEmptySpacePlacementPlanner {

    private static final int SEARCH_RANGE = 3;
    private static final Direction[] SUPPORT_FACE_PRIORITY = {
            Direction.UP,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.DOWN
    };

    public Optional<CarriedBlockPlacementPlanner.PlacementTarget> findNearest(AltoClef mod, BlockState carriedState) {
        List<CarriedBlockPlacementPlanner.PlacementTarget> candidates = findNearestCandidates(mod, carriedState, 1);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    public List<CarriedBlockPlacementPlanner.PlacementTarget> findNearestCandidates(AltoClef mod, BlockState carriedState, int limit) {
        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        BlockPos playerFeet = ctx.playerFeet();
        List<CarriedBlockPlacementPlanner.PlacementTarget> candidates = new ArrayList<>();

        for (int x = -SEARCH_RANGE; x <= SEARCH_RANGE; ++x) {
            for (int y = -1; y <= 2; ++y) {
                for (int z = -SEARCH_RANGE; z <= SEARCH_RANGE; ++z) {
                    BlockPos placePos = playerFeet.add(x, y, z);
                    Optional<CarriedBlockPlacementPlanner.PlacementTarget> candidate = findBestSupport(mod, placePos);
                    candidate.ifPresent(candidates::add);
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(CarriedBlockPlacementPlanner.PlacementTarget::score));
        if (limit > 0 && candidates.size() > limit) {
            return new ArrayList<>(candidates.subList(0, limit));
        }
        return candidates;
    }

    public boolean isValid(AltoClef mod, CarriedBlockPlacementPlanner.PlacementTarget target, BlockState carriedState) {
        if (target == null) {
            return false;
        }
        if (!isBlockEmptyForPlacement(mod, target.placePos())) {
            return false;
        }
        return canUseSupport(mod, target.supportPos(), target.supportFace());
    }

    private Optional<CarriedBlockPlacementPlanner.PlacementTarget> findBestSupport(AltoClef mod, BlockPos placePos) {
        if (!isBlockEmptyForPlacement(mod, placePos)) {
            return Optional.empty();
        }

        CarriedBlockPlacementPlanner.PlacementTarget best = null;
        for (Direction supportFace : SUPPORT_FACE_PRIORITY) {
            BlockPos supportPos = placePos.offset(supportFace.getOpposite());
            if (!canUseSupport(mod, supportPos, supportFace)) {
                continue;
            }
            double score = score(mod, placePos, supportFace);
            CarriedBlockPlacementPlanner.PlacementTarget target =
                    new CarriedBlockPlacementPlanner.PlacementTarget(placePos, supportPos, supportFace, score);
            if (best == null || target.score() < best.score()) {
                best = target;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isBlockEmptyForPlacement(AltoClef mod, BlockPos placePos) {
        if (mod.getExtraBaritoneSettings().shouldAvoidPlacingAt(placePos)) {
            return false;
        }
        if (isPlayerBodyBlock(mod, placePos)) {
            return false;
        }

        BlockState existingState = mod.getWorld().getBlockState(placePos);
        return existingState.isAir() || existingState.isReplaceable();
    }

    private boolean canUseSupport(AltoClef mod, BlockPos supportPos, Direction supportFace) {
        if (!WorldHelper.canReach(supportPos)) {
            return false;
        }
        if (!MovementHelper.canPlaceAgainst(mod.getClientBaritone().getPlayerContext(), supportPos)) {
            return false;
        }
        return isWithinInteractionReach(mod, supportPos, supportFace);
    }

    private boolean isWithinInteractionReach(AltoClef mod, BlockPos supportPos, Direction supportFace) {
        double reach = mod.getClientBaritone().getPlayerContext().playerController().getBlockReachDistance();
        Vec3d hitPos = Vec3d.ofCenter(supportPos).add(
                supportFace.getOffsetX() * 0.5,
                supportFace.getOffsetY() * 0.5,
                supportFace.getOffsetZ() * 0.5);
        return mod.getPlayer().getCameraPosVec(1.0F).squaredDistanceTo(hitPos) <= reach * reach;
    }

    private boolean isPlayerBodyBlock(AltoClef mod, BlockPos placePos) {
        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        BlockPos playerFeet = ctx.playerFeet();
        return placePos.equals(playerFeet) || placePos.equals(playerFeet.up());
    }

    private double score(AltoClef mod, BlockPos placePos, Direction supportFace) {
        double distance = BlockPosVer.getSquaredDistance(placePos, mod.getPlayer().getPos());
        double floorPenalty = supportFace == Direction.UP ? 0 : 2;
        double ceilingPenalty = supportFace == Direction.DOWN ? 6 : 0;
        return distance + floorPenalty + ceilingPenalty;
    }
}
