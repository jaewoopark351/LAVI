package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.Optional;
import java.util.List;

//20260902_kpopmodder: Return one sealed scope/owner exit without consulting ambient command state.
public record FabricChatClefCraftResourceContainerOwnerExitDecision(
        Optional<IronPickaxeAcquisitionScopeKey> scopeKey,
        Optional<FabricChatClefCraftResourceContainerActiveTarget> closedActiveTarget,
        boolean sourceEmissionCompleted,
        boolean sourceTransitionSuppressed,
        boolean ownerIdentityAmbiguous,
        List<IronPickaxeAcquisitionScopeKey> ambiguousScopeKeys) {
    public FabricChatClefCraftResourceContainerOwnerExitDecision {
        scopeKey = scopeKey == null ? Optional.empty() : scopeKey;
        closedActiveTarget = closedActiveTarget == null
                ? Optional.empty()
                : closedActiveTarget;
        ambiguousScopeKeys = ambiguousScopeKeys == null
                ? List.of()
                : List.copyOf(ambiguousScopeKeys);
    }
}
