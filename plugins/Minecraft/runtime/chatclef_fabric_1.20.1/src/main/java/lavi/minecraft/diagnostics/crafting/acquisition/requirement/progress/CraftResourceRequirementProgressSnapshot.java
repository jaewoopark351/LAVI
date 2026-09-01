package lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;

import java.util.List;
import java.util.Objects;

//20260901_kpopmodder: Expose bounded semantic requirement transitions for terminal evidence.
public record CraftResourceRequirementProgressSnapshot(
        long requirementTransitionCount,
        CraftResourceStage resourceStage,
        String activeRequirementItem,
        long activeRequirementCount,
        List<String> boundedRequirementHistory,
        long omittedRequirementHistoryCount,
        long firstObservedTick,
        long lastObservedTick,
        long suppressedDetailCount,
        long unownedObservationCount,
        long unknownAssociationCount,
        boolean counterSaturated) {
    public CraftResourceRequirementProgressSnapshot {
        resourceStage = Objects.requireNonNull(resourceStage, "resourceStage");
        activeRequirementItem = Objects.requireNonNull(
                activeRequirementItem,
                "activeRequirementItem"
        );
        boundedRequirementHistory = List.copyOf(Objects.requireNonNull(
                boundedRequirementHistory,
                "boundedRequirementHistory"
        ));
    }
}
