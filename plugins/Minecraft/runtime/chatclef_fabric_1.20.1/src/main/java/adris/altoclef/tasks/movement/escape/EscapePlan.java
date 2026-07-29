package adris.altoclef.tasks.movement.escape;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//20260729_kpopmodder: Added this plan data object so terrain escape planning can be refactored without changing actions.
public class EscapePlan {
    private final BlockPos origin;
    private final Direction direction;
    private final String kind;
    private final List<BlockPos> blocksToClear;

    EscapePlan(BlockPos origin, Direction direction, String kind, List<BlockPos> blocksToClear) {
        this.origin = origin;
        this.direction = direction;
        this.kind = kind;
        this.blocksToClear = List.copyOf(blocksToClear);
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getKind() {
        return kind;
    }

    public List<BlockPos> getBlocksToClear() {
        return blocksToClear;
    }

    public String describe() {
        return "kind=" + kind
                + ", origin=" + origin.toShortString()
                + ", direction=" + direction.getName()
                + ", blocks=" + describeBlocks();
    }

    private String describeBlocks() {
        List<String> blockStrings = new ArrayList<>();
        for (BlockPos block : blocksToClear) {
            blockStrings.add(block.toShortString());
        }
        return blockStrings.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EscapePlan plan)) {
            return false;
        }
        return origin.equals(plan.origin)
                && direction == plan.direction
                && kind.equals(plan.kind)
                && blocksToClear.equals(plan.blocksToClear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, direction, kind, blocksToClear);
    }
}
