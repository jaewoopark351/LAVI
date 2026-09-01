package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;

//20260901_kpopmodder: Bind an interaction pair to one already-proven target attempt.
public record FabricChatClefCraftResourceInteractionAttemptReference(
        long interactionId,
        IronPickaxeAcquisitionScopeKey scopeKey,
        long targetAttemptSequence,
        CraftResourceTargetTuple targetTuple,
        String sourceTargetBlockId,
        String sourceTargetKind,
        long headClientTick) {
    public FabricChatClefCraftResourceInteractionAttemptReference {
        if (interactionId < 0L || targetAttemptSequence <= 0L) {
            throw new IllegalArgumentException("interaction and attempt identities must exist");
        }
        scopeKey = Objects.requireNonNull(scopeKey, "scopeKey");
        targetTuple = Objects.requireNonNull(targetTuple, "targetTuple");
        sourceTargetBlockId = unavailableIfBlank(sourceTargetBlockId);
        sourceTargetKind = unavailableIfBlank(sourceTargetKind);
    }

    private static String unavailableIfBlank(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
