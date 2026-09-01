package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.util.Objects;

//20260901_kpopmodder: Retain a bounded first-and-recent semantic target history.
public record CraftResourceTargetHistorySample(
        long targetAttemptSequence,
        CraftResourceTargetTuple targetTuple) {

    public CraftResourceTargetHistorySample {
        targetTuple = Objects.requireNonNull(targetTuple, "targetTuple");
    }
}
