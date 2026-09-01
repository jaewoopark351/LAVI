package lavi.minecraft.diagnostics.crafting.acquisition.target.failure;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;

import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Expose a bounded command-owned failure aggregate for terminal evidence.
public record CraftResourceFailureAggregateSnapshot(
        String ownerTaskClass,
        long targetAttemptSequence,
        CraftResourceAssociationStatus associationStatus,
        CraftResourceStage resourceStage,
        CraftResourceTargetRole targetRole,
        String targetPosition,
        long unreachableRequestCount,
        long firstFailureCount,
        long lastFailureCount,
        long allowedFailures,
        Optional<Boolean> unreachableBefore,
        Optional<Boolean> unreachableAfter,
        long blacklistTransitionCount,
        long firstObservedTick,
        long lastObservedTick,
        long suppressedDetailCount,
        long unownedObservationCount,
        long unknownAssociationCount,
        boolean counterSaturated) {
    public CraftResourceFailureAggregateSnapshot {
        ownerTaskClass = Objects.requireNonNull(ownerTaskClass, "ownerTaskClass");
        associationStatus = Objects.requireNonNull(associationStatus, "associationStatus");
        resourceStage = Objects.requireNonNull(resourceStage, "resourceStage");
        targetRole = Objects.requireNonNull(targetRole, "targetRole");
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        unreachableBefore = Objects.requireNonNull(
                unreachableBefore,
                "unreachableBefore"
        );
        unreachableAfter = Objects.requireNonNull(unreachableAfter, "unreachableAfter");
    }
}
