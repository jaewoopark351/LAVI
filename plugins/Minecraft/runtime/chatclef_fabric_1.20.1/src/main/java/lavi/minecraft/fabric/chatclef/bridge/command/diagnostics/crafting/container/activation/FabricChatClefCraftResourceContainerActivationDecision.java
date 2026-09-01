package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import java.util.Optional;

//20260901_kpopmodder: Return identity-only reconciliation facts to the ledger projector.
public record FabricChatClefCraftResourceContainerActivationDecision(
        Optional<FabricChatClefCraftResourceContainerActiveTarget> closedActiveTarget,
        Optional<FabricChatClefCraftResourceContainerActivationCandidate> installedCandidate,
        boolean sourceEmissionCompleted,
        boolean sourceTransitionSuppressed,
        boolean identityMismatchObserved) {
    public FabricChatClefCraftResourceContainerActivationDecision {
        closedActiveTarget = closedActiveTarget == null
                ? Optional.empty()
                : closedActiveTarget;
        installedCandidate = installedCandidate == null
                ? Optional.empty()
                : installedCandidate;
    }
}
