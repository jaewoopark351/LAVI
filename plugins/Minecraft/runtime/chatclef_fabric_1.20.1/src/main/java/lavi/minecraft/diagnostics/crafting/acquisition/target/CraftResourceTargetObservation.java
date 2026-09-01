package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;

import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Carry immutable target evidence into bounded diagnostic accounting.
public record CraftResourceTargetObservation(
        CraftResourceAssociationStatus associationStatus,
        CraftResourceTargetObservationKind observationKind,
        Optional<CraftResourceTargetTuple> targetTuple) {

    public CraftResourceTargetObservation {
        associationStatus = Objects.requireNonNull(associationStatus, "associationStatus");
        observationKind = Objects.requireNonNull(observationKind, "observationKind");
        targetTuple = Objects.requireNonNull(targetTuple, "targetTuple");
    }
}
