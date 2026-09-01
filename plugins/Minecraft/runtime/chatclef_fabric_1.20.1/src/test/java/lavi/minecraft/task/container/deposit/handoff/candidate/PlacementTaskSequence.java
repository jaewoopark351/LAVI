package lavi.minecraft.task.container.deposit.handoff.candidate;

import java.util.Arrays;
import java.util.Objects;

//20260831_kpopmodder: Supply one fresh controlled placement identity for each admitted generation.
final class PlacementTaskSequence {
    private final PlacementTaskProbe[] placements;
    private int nextIndex;

    PlacementTaskSequence(PlacementTaskProbe... placements) {
        Objects.requireNonNull(placements, "placements");
        if (placements.length == 0) {
            throw new IllegalArgumentException("At least one placement probe is required");
        }
        this.placements = Arrays.copyOf(placements, placements.length);
        for (PlacementTaskProbe placement : this.placements) {
            Objects.requireNonNull(placement, "placement");
        }
    }

    PlacementTaskProbe next() {
        if (nextIndex >= placements.length) {
            throw new AssertionError("No controlled placement identity remains");
        }
        return placements[nextIndex++];
    }

    int creationCalls() {
        return nextIndex;
    }
}
