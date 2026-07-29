package adris.altoclef.tasks.movement.escape;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260729_kpopmodder: Added this data object so escape plans can describe future block placement without executing it.
public class EscapePlaceCandidate {
    private final BlockPos target;
    private final BlockPos support;
    private final String reason;

    public EscapePlaceCandidate(BlockPos target, BlockPos support, String reason) {
        this.target = Objects.requireNonNull(target);
        this.support = Objects.requireNonNull(support);
        this.reason = Objects.requireNonNull(reason);
    }

    public BlockPos getTarget() {
        return target;
    }

    public BlockPos getSupport() {
        return support;
    }

    public String getReason() {
        return reason;
    }

    public String describe() {
        return "target=" + target.toShortString()
                + ", support=" + support.toShortString()
                + ", reason=" + reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EscapePlaceCandidate candidate)) {
            return false;
        }
        return target.equals(candidate.target)
                && support.equals(candidate.support)
                && reason.equals(candidate.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, support, reason);
    }
}
