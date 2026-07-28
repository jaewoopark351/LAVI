package adris.altoclef.tasks.resources.mining;

import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260729_kpopmodder: Added this choice object to keep mining/drop target decisions explicit.
final class MiningTargetChoice {
    enum Kind {
        BLOCK,
        DROP,
        MANAGED_PICKUP,
        NONE
    }

    enum Reason {
        INTERACTION_PAUSED_DROP_PREFERRED,
        PICKUP_COORDINATOR_PREFERRED,
        DROP_INTERRUPTS_MINING_TARGET,
        RETAIN_MINING_TARGET,
        LOCAL_MINING_SESSION,
        CLOSEST_DROP,
        CLOSEST_BLOCK
    }

    private final Kind kind;
    private final Reason reason;
    private final Object target;

    private MiningTargetChoice(Kind kind, Reason reason, Object target) {
        this.kind = kind;
        this.reason = reason;
        this.target = target;
    }

    static MiningTargetChoice block(BlockPos block, Reason reason) {
        return new MiningTargetChoice(block == null ? Kind.NONE : Kind.BLOCK, reason, block);
    }

    static MiningTargetChoice drop(ItemEntity drop, Reason reason) {
        return new MiningTargetChoice(drop == null ? Kind.NONE : Kind.DROP, reason, drop);
    }

    static MiningTargetChoice target(Object target, Reason reason) {
        return new MiningTargetChoice(kindOf(target), reason, target);
    }

    static MiningTargetChoice empty(Reason reason) {
        return new MiningTargetChoice(Kind.NONE, reason, null);
    }

    Optional<Object> target() {
        return Optional.ofNullable(target);
    }

    Kind kind() {
        return kind;
    }

    Reason reason() {
        return reason;
    }

    private static Kind kindOf(Object target) {
        if (target instanceof BlockPos) {
            return Kind.BLOCK;
        }
        if (target instanceof ItemEntity) {
            return Kind.DROP;
        }
        if (target != null) {
            return Kind.MANAGED_PICKUP;
        }
        return Kind.NONE;
    }
}
