package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;

//20260901_kpopmodder: Retain one candidate reference and saturating local detail accounting.
record FabricChatClefCraftResourceReferenceState(
        CraftResourceTargetTuple current,
        long transitionCount,
        long detailEligibleTransitionCount,
        long suppressedDetailTransitionCount,
        boolean counterSaturated
) {
    FabricChatClefCraftResourceReferenceState {
        Objects.requireNonNull(current, "current");
    }
}
