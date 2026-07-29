package adris.altoclef.tasks.construction.destroy;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260729_kpopmodder: Added this snapshot to log destroy progress without changing MovementProgressChecker behavior.
public final class DestroyProgressTracker {
    private DestroyProgressTracker() {
    }

    public static ProgressSnapshot capture(AltoClef mod, BlockPos target, boolean taskMining,
                                           boolean unstuckTaskActive) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(target, "target");

        boolean controllerBreaking = mod.getControllerExtras().isBreakingBlock();
        BlockPos breakingPos = controllerBreaking ? mod.getControllerExtras().getBreakingBlockPos() : null;
        boolean alreadyBreakingTarget = controllerBreaking && target.equals(breakingPos);
        boolean baritonePathing = mod.getClientBaritone().getPathingBehavior().isPathing();
        boolean customGoalActive = mod.getClientBaritone().getCustomGoalProcess().isActive();
        boolean builderActive = mod.getClientBaritone().getBuilderProcess().isActive();
        boolean closeToMoveBack = target.isWithinDistance(mod.getPlayer().getPos(), 2);

        return new ProgressSnapshot(taskMining, unstuckTaskActive, controllerBreaking, alreadyBreakingTarget,
                breakingPos, baritonePathing, customGoalActive, builderActive, closeToMoveBack);
    }

    public static final class ProgressSnapshot {
        private final boolean taskMining;
        private final boolean unstuckTaskActive;
        private final boolean controllerBreaking;
        private final boolean alreadyBreakingTarget;
        private final BlockPos breakingPos;
        private final boolean baritonePathing;
        private final boolean customGoalActive;
        private final boolean builderActive;
        private final boolean closeToMoveBack;

        private ProgressSnapshot(boolean taskMining, boolean unstuckTaskActive, boolean controllerBreaking,
                                 boolean alreadyBreakingTarget, BlockPos breakingPos, boolean baritonePathing,
                                 boolean customGoalActive, boolean builderActive, boolean closeToMoveBack) {
            this.taskMining = taskMining;
            this.unstuckTaskActive = unstuckTaskActive;
            this.controllerBreaking = controllerBreaking;
            this.alreadyBreakingTarget = alreadyBreakingTarget;
            this.breakingPos = breakingPos;
            this.baritonePathing = baritonePathing;
            this.customGoalActive = customGoalActive;
            this.builderActive = builderActive;
            this.closeToMoveBack = closeToMoveBack;
        }

        String stateKey() {
            return "taskMining=" + taskMining
                    + ",unstuck=" + unstuckTaskActive
                    + ",breaking=" + controllerBreaking
                    + ",sameTarget=" + alreadyBreakingTarget
                    + ",pathing=" + baritonePathing
                    + ",goal=" + customGoalActive
                    + ",builder=" + builderActive
                    + ",close=" + closeToMoveBack;
        }

        String describe() {
            return "taskMining=" + taskMining
                    + ", unstuckTaskActive=" + unstuckTaskActive
                    + ", controllerBreaking=" + controllerBreaking
                    + ", alreadyBreakingTarget=" + alreadyBreakingTarget
                    + ", breakingPos=" + describePos(breakingPos)
                    + ", baritonePathing=" + baritonePathing
                    + ", customGoalActive=" + customGoalActive
                    + ", builderActive=" + builderActive
                    + ", closeToMoveBack=" + closeToMoveBack;
        }

        private String describePos(BlockPos pos) {
            return pos == null ? "none" : pos.toShortString();
        }
    }
}
