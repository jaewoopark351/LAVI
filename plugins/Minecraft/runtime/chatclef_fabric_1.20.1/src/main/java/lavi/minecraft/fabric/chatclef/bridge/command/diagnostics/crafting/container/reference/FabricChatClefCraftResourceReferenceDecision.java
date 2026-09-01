package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Separate semantic-reference changes from active target attempts.
public record FabricChatClefCraftResourceReferenceDecision(
        boolean changed,
        boolean retained,
        boolean detailEligible,
        Optional<CraftResourceTargetTuple> previous,
        CraftResourceTargetTuple current,
        long transitionCount,
        long detailEligibleTransitionCount,
        long suppressedDetailTransitionCount,
        boolean counterSaturated) {
    public FabricChatClefCraftResourceReferenceDecision {
        previous = Objects.requireNonNull(previous, "previous");
        current = Objects.requireNonNull(current, "current");
    }
}
