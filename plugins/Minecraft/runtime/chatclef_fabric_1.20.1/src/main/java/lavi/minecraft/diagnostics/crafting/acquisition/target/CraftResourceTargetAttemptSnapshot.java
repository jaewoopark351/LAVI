package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.target.closure.CraftResourceTargetClosureSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Expose immutable bounded target-attempt aggregates for terminal evidence.
public record CraftResourceTargetAttemptSnapshot(
        long targetAttemptSequence,
        long attemptTransitionCount,
        long detailEligibleTransitionCount,
        List<CraftResourceTargetHistorySample> history,
        long omittedTargetSampleCount,
        Optional<CraftResourceTargetTuple> currentTuple,
        Optional<CraftResourceTargetClosureSnapshot> lastClosure,
        long unreachableRequestCount,
        long blacklistTransitionCount,
        long unownedObservationCount,
        long unknownAssociationCount,
        boolean counterSaturated) {

    public CraftResourceTargetAttemptSnapshot {
        history = List.copyOf(Objects.requireNonNull(history, "history"));
        currentTuple = Objects.requireNonNull(currentTuple, "currentTuple");
        lastClosure = Objects.requireNonNull(lastClosure, "lastClosure");
    }
}
