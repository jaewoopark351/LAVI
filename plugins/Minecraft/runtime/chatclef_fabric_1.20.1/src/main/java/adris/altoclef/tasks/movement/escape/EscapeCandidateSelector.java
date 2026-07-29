package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//20260729_kpopmodder: Added this selector to separate escape candidate ordering and cooldown checks from task execution.
public class EscapeCandidateSelector {
    private final EscapeStepPlanner stepPlanner;

    public EscapeCandidateSelector() {
        this(new EscapeStepPlanner(new EscapeBlockActionPolicy()));
    }

    EscapeCandidateSelector(EscapeStepPlanner stepPlanner) {
        this.stepPlanner = stepPlanner;
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        if (mod.getPlayer() == null) {
            return Optional.empty();
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            return Optional.empty();
        }

        List<Direction> directions = orderedDirections(mod);
        for (Direction direction : directions) {
            Optional<EscapePlan> stairPlan = stepPlanner.buildStairPlan(mod, origin, direction);
            if (stairPlan.isPresent()) {
                return stairPlan;
            }
        }
        for (Direction direction : directions) {
            Optional<EscapePlan> sidePlan = stepPlanner.buildSidePocketPlan(mod, origin, direction);
            if (sidePlan.isPresent()) {
                return sidePlan;
            }
        }
        return stepPlanner.buildVerticalHeadroomPlan(mod, origin);
    }

    private boolean isOriginOnCooldown(BlockPos origin, Set<BlockPos> cooldownOrigins) {
        return cooldownOrigins != null && cooldownOrigins.contains(origin);
    }

    private List<Direction> orderedDirections(AltoClef mod) {
        Direction facing = mod.getPlayer().getHorizontalFacing();
        List<Direction> result = new ArrayList<>();
        addDirection(result, facing);
        addDirection(result, facing.rotateYClockwise());
        addDirection(result, facing.rotateYCounterclockwise());
        addDirection(result, facing.getOpposite());
        addDirection(result, Direction.NORTH);
        addDirection(result, Direction.SOUTH);
        addDirection(result, Direction.EAST);
        addDirection(result, Direction.WEST);
        return result;
    }

    private void addDirection(List<Direction> directions, Direction direction) {
        if (direction != null && direction.getAxis().isHorizontal() && !directions.contains(direction)) {
            directions.add(direction);
        }
    }
}
