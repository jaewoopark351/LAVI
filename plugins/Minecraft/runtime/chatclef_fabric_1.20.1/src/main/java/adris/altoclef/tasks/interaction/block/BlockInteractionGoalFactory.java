package adris.altoclef.tasks.interaction.block;

import adris.altoclef.util.baritone.GoalAnd;
import adris.altoclef.util.baritone.GoalBlockSide;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalTwoBlocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

//20260730_kpopmodder: Added this factory to isolate block-interaction movement goal selection from task flow.
final class BlockInteractionGoalFactory {

    private BlockInteractionGoalFactory() {
    }

    static Goal createGoalForInteract(BlockPos target,
                                      int reachDistance,
                                      Direction interactSide,
                                      Vec3i interactOffset,
                                      boolean walkInto) {
        boolean sideMatters = interactSide != null;
        if (sideMatters) {
            Vec3i offs = interactSide.getVector();
            if (offs.getY() == -1) {
                // If we're below, place ourselves two blocks below.
                offs = offs.down();
            }
            target = target.add(offs);
        }

        if (walkInto) {
            return new GoalTwoBlocks(target);
        }
        if (sideMatters) {
            // Make sure we're on the right side of the block.
            Goal sideGoal = new GoalBlockSide(target, interactSide, 1);
            return new GoalAnd(sideGoal, new GoalNear(target.add(interactOffset), reachDistance));
        }

        // TODO: Cleaner method of picking which side to approach from. This is only here for the lava stuff.
        return new GoalTwoBlocks(target.up());
        //return new GoalNear(target.add(interactOffset), reachDistance);
    }
}
