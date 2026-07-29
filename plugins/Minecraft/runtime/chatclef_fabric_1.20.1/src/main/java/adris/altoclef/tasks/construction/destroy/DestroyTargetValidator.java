package adris.altoclef.tasks.construction.destroy;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

//20260729_kpopmodder: Added this validator to observe destroy-target safety before changing DestroyBlockTask behavior.
public final class DestroyTargetValidator {
    private DestroyTargetValidator() {
    }

    public static TargetDecision evaluate(AltoClef mod, BlockPos target) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(target, "target");

        BlockState state = mod.getWorld().getBlockState(target);
        boolean blockAboveSolid = WorldHelper.isSolidBlock(target.up());
        Vec3d playerCheckPos = mod.getPlayer().isOnGround()
                ? mod.getPlayer().getPos()
                : mod.getPlayer().getPos().add(0, -1, 0);
        boolean playerAboveTarget = mod.getPlayer().getPos().y > target.getY();
        boolean playerCloseToTarget = target.isWithinDistance(playerCheckPos, 0.89);
        boolean dangerousRightAbove = !blockAboveSolid
                && playerAboveTarget
                && playerCloseToTarget
                && WorldHelper.dangerousToBreakIfRightAbove(target);

        if (state.isAir()) {
            return new TargetDecision(target, state, false, "already_air", "target is already air",
                    blockAboveSolid, playerAboveTarget, playerCloseToTarget, dangerousRightAbove);
        }
        if (dangerousRightAbove) {
            return new TargetDecision(target, state, false, "dangerous_right_above",
                    "current task would move away before breaking", blockAboveSolid, playerAboveTarget,
                    playerCloseToTarget, true);
        }
        return new TargetDecision(target, state, true, "allowed_by_current_rules",
                "current task can attempt this block", blockAboveSolid, playerAboveTarget,
                playerCloseToTarget, false);
    }

    public static final class TargetDecision {
        private final BlockPos target;
        private final BlockState state;
        private final boolean canAttemptDestroy;
        private final String reasonKey;
        private final String reason;
        private final boolean blockAboveSolid;
        private final boolean playerAboveTarget;
        private final boolean playerCloseToTarget;
        private final boolean dangerousRightAbove;

        private TargetDecision(BlockPos target, BlockState state, boolean canAttemptDestroy, String reasonKey,
                               String reason, boolean blockAboveSolid, boolean playerAboveTarget,
                               boolean playerCloseToTarget, boolean dangerousRightAbove) {
            this.target = target;
            this.state = state;
            this.canAttemptDestroy = canAttemptDestroy;
            this.reasonKey = reasonKey;
            this.reason = reason;
            this.blockAboveSolid = blockAboveSolid;
            this.playerAboveTarget = playerAboveTarget;
            this.playerCloseToTarget = playerCloseToTarget;
            this.dangerousRightAbove = dangerousRightAbove;
        }

        public BlockPos target() {
            return target;
        }

        public BlockState state() {
            return state;
        }

        public boolean canAttemptDestroy() {
            return canAttemptDestroy;
        }

        public String reasonKey() {
            return reasonKey;
        }

        public String describeBlock() {
            return state.getBlock().getTranslationKey();
        }

        String describe() {
            return "target=" + target.toShortString()
                    + ", block=" + describeBlock()
                    + ", canAttemptDestroy=" + canAttemptDestroy
                    + ", reason=" + reason
                    + ", blockAboveSolid=" + blockAboveSolid
                    + ", playerAboveTarget=" + playerAboveTarget
                    + ", playerCloseToTarget=" + playerCloseToTarget
                    + ", dangerousRightAbove=" + dangerousRightAbove;
        }
    }
}
