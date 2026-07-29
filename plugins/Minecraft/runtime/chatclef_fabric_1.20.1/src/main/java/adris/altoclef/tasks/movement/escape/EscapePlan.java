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
    private final List<EscapePlaceCandidate> placeCandidates;

    EscapePlan(BlockPos origin, Direction direction, String kind, List<BlockPos> blocksToClear) {
        this(origin, direction, kind, blocksToClear, List.of());
    }

    EscapePlan(BlockPos origin, Direction direction, String kind, List<BlockPos> blocksToClear,
               List<EscapePlaceCandidate> placeCandidates) {
        this.origin = origin;
        this.direction = direction;
        this.kind = kind;
        this.blocksToClear = List.copyOf(blocksToClear);
        this.placeCandidates = List.copyOf(placeCandidates);
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

    public List<EscapePlaceCandidate> getPlaceCandidates() {
        return placeCandidates;
    }

    public int getClearBlockCount() {
        return blocksToClear.size();
    }

    public boolean hasBlocksToClear() {
        return !blocksToClear.isEmpty();
    }

    public int getPlaceCandidateCount() {
        return placeCandidates.size();
    }

    public boolean hasPlaceCandidates() {
        return !placeCandidates.isEmpty();
    }

    public boolean isClearComplete(int clearIndex) {
        return clearIndex >= blocksToClear.size();
    }

    public BlockPos getBlockToClear(int clearIndex) {
        return blocksToClear.get(clearIndex);
    }

    public String describeClearProgress(int clearIndex) {
        return (clearIndex + 1) + "/" + getClearBlockCount();
    }

    public String describe() {
        return "kind=" + kind
                + ", origin=" + origin.toShortString()
                + ", direction=" + direction.getName()
                + ", blocks=" + describeBlocks()
                + ", placeCandidates=" + describePlaceCandidates();
    }

    private String describeBlocks() {
        List<String> blockStrings = new ArrayList<>();
        for (BlockPos block : blocksToClear) {
            blockStrings.add(block.toShortString());
        }
        return blockStrings.toString();
    }

    private String describePlaceCandidates() {
        List<String> placeStrings = new ArrayList<>();
        for (EscapePlaceCandidate candidate : placeCandidates) {
            placeStrings.add(candidate.describe());
        }
        return placeStrings.toString();
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
                && blocksToClear.equals(plan.blocksToClear)
                && placeCandidates.equals(plan.placeCandidates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, direction, kind, blocksToClear, placeCandidates);
    }
}
