package adris.altoclef.tasks.movement.escape;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

//20260729_kpopmodder: Added this key so failed escape candidates can cool down by route kind and direction.
public class EscapeCandidateKey {
    private final BlockPos origin;
    private final String kind;
    private final Direction direction;

    public EscapeCandidateKey(BlockPos origin, String kind, Direction direction) {
        this.origin = Objects.requireNonNull(origin).toImmutable();
        this.kind = Objects.requireNonNull(kind);
        this.direction = Objects.requireNonNull(direction);
    }

    public static EscapeCandidateKey fromPlan(EscapePlan plan) {
        Objects.requireNonNull(plan);
        return new EscapeCandidateKey(plan.getOrigin(), plan.getKind(), plan.getDirection());
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public String getKind() {
        return kind;
    }

    public Direction getDirection() {
        return direction;
    }

    public String describe() {
        return "origin=" + origin.toShortString()
                + ", kind=" + kind
                + ", direction=" + direction.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EscapeCandidateKey key)) {
            return false;
        }
        return origin.equals(key.origin)
                && kind.equals(key.kind)
                && direction == key.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, kind, direction);
    }
}
