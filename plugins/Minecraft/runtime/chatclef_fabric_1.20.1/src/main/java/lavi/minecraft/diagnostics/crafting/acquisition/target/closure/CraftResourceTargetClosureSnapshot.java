package lavi.minecraft.diagnostics.crafting.acquisition.target.closure;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;

//20260901_kpopmodder: Retain the last owned target at its observed closure boundary.
public record CraftResourceTargetClosureSnapshot(
        long targetAttemptSequence,
        CraftResourceTargetTuple targetTuple,
        CraftResourceTargetObservationKind closureKind) {
    public CraftResourceTargetClosureSnapshot {
        targetTuple = Objects.requireNonNull(targetTuple, "targetTuple");
        closureKind = Objects.requireNonNull(closureKind, "closureKind");
    }
}
