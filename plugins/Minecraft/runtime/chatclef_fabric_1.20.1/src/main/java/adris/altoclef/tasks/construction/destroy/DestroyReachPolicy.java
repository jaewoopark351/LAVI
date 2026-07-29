package adris.altoclef.tasks.construction.destroy;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.Rotation;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

//20260729_kpopmodder: Added this reach policy to expose reach, line-of-sight, and cancel guards before behavior changes.
public final class DestroyReachPolicy {
    private DestroyReachPolicy() {
    }

    public static ReachDecision evaluate(AltoClef mod, BlockPos target,
                                         DestroyTargetValidator.TargetDecision targetDecision) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(targetDecision, "targetDecision");

        Optional<Rotation> reach = LookHelper.getReach(target);
        double reachDistance = mod.getClientBaritone().getPlayerContext().playerController().getBlockReachDistance();
        boolean cleanLineOfSight = LookHelper.cleanLineOfSight(mod.getPlayer(), target, reachDistance);
        boolean onGround = mod.getPlayer().isOnGround();
        boolean touchingWater = mod.getPlayer().isTouchingWater();
        boolean needsToEat = mod.getFoodChain().needsToEat();
        boolean inNetherPortal = WorldHelper.isInNetherPortal();
        boolean safeToCancel = mod.getClientBaritone().getPathingBehavior().isSafeToCancel();

        boolean readyToMine = targetDecision.canAttemptDestroy()
                && reach.isPresent()
                && (touchingWater || onGround)
                && !needsToEat
                && !inNetherPortal
                && safeToCancel;

        String reasonKey = selectReasonKey(targetDecision, reach, onGround, touchingWater, needsToEat,
                inNetherPortal, safeToCancel);

        return new ReachDecision(reach, reachDistance, cleanLineOfSight, onGround, touchingWater, needsToEat,
                inNetherPortal, safeToCancel, readyToMine, reasonKey);
    }

    private static String selectReasonKey(DestroyTargetValidator.TargetDecision targetDecision,
                                          Optional<Rotation> reach,
                                          boolean onGround,
                                          boolean touchingWater,
                                          boolean needsToEat,
                                          boolean inNetherPortal,
                                          boolean safeToCancel) {
        if (!targetDecision.canAttemptDestroy()) {
            return "target_" + targetDecision.reasonKey();
        }
        if (reach.isEmpty()) {
            return "no_reach_rotation";
        }
        if (!touchingWater && !onGround) {
            return "not_grounded_or_in_water";
        }
        if (needsToEat) {
            return "food_chain_needs_to_eat";
        }
        if (inNetherPortal) {
            return "inside_nether_portal";
        }
        if (!safeToCancel) {
            return "baritone_not_safe_to_cancel";
        }
        return "ready_to_mine";
    }

    public static final class ReachDecision {
        private final Optional<Rotation> reach;
        private final double reachDistance;
        private final boolean cleanLineOfSight;
        private final boolean onGround;
        private final boolean touchingWater;
        private final boolean needsToEat;
        private final boolean inNetherPortal;
        private final boolean safeToCancel;
        private final boolean readyToMine;
        private final String reasonKey;

        private ReachDecision(Optional<Rotation> reach, double reachDistance, boolean cleanLineOfSight,
                              boolean onGround, boolean touchingWater, boolean needsToEat, boolean inNetherPortal,
                              boolean safeToCancel, boolean readyToMine, String reasonKey) {
            this.reach = reach;
            this.reachDistance = reachDistance;
            this.cleanLineOfSight = cleanLineOfSight;
            this.onGround = onGround;
            this.touchingWater = touchingWater;
            this.needsToEat = needsToEat;
            this.inNetherPortal = inNetherPortal;
            this.safeToCancel = safeToCancel;
            this.readyToMine = readyToMine;
            this.reasonKey = reasonKey;
        }

        String stateKey() {
            return reasonKey
                    + ",reach=" + reach.isPresent()
                    + ",los=" + cleanLineOfSight
                    + ",ground=" + onGround
                    + ",water=" + touchingWater
                    + ",food=" + needsToEat
                    + ",portal=" + inNetherPortal
                    + ",safeCancel=" + safeToCancel;
        }

        String describe() {
            return "readyToMine=" + readyToMine
                    + ", reason=" + reasonKey
                    + ", reach=" + describeReach()
                    + ", lineOfSight=" + cleanLineOfSight
                    + ", reachDistance=" + formatDouble(reachDistance)
                    + ", onGround=" + onGround
                    + ", touchingWater=" + touchingWater
                    + ", needsToEat=" + needsToEat
                    + ", inNetherPortal=" + inNetherPortal
                    + ", safeToCancel=" + safeToCancel;
        }

        private String describeReach() {
            return reach.map(rotation -> "yaw=" + formatDouble(rotation.getYaw())
                            + ", pitch=" + formatDouble(rotation.getPitch()))
                    .orElse("none");
        }

        private String formatDouble(double value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
    }
}
