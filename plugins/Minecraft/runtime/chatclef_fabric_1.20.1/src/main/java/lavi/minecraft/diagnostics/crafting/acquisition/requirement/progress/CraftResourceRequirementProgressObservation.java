package lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;

import java.util.Objects;

//20260901_kpopmodder: Carry only already-observed requirement facts into a command ledger.
public record CraftResourceRequirementProgressObservation(
        CraftResourceAssociationStatus associationStatus,
        CraftResourceStage resourceStage,
        String activeRequirementItem,
        long activeRequirementCount,
        String sourceEventName,
        long observedTick,
        boolean sourceEmissionCompleted) {
    public CraftResourceRequirementProgressObservation {
        associationStatus = Objects.requireNonNull(
                associationStatus,
                "associationStatus"
        );
        resourceStage = Objects.requireNonNull(resourceStage, "resourceStage");
        activeRequirementItem = bounded(activeRequirementItem);
        sourceEventName = bounded(sourceEventName);
    }

    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "UNAVAILABLE";
        }
        String normalized = value.trim();
        return normalized.length() <= 256 ? normalized : normalized.substring(0, 256);
    }
}
