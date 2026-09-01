package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.matching;

//20260901_kpopmodder: Return a fail-closed tuple match without mutating target state.
public record FabricChatClefCraftResourceInteractionTupleMatch(
        boolean matched,
        String observationGapReason) {
    public FabricChatClefCraftResourceInteractionTupleMatch {
        observationGapReason = observationGapReason == null
                || observationGapReason.isBlank()
                        ? "UNAVAILABLE"
                        : observationGapReason;
    }
}
