package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;

import java.util.Optional;

//20260901_kpopmodder: Carry one same-boundary Task ownership classification into projections.
public record FabricChatClefCraftResourceAssociationSnapshot(
        Optional<IronPickaxeAcquisitionScopeBinding> binding,
        CraftResourceAssociationDecision decision,
        long captureClientTick,
        String captureThread,
        String selectedChainClass,
        String selectedChainInstanceId,
        String sourceTaskClass,
        String sourceTaskInstanceId,
        boolean emittingTaskPresentInSelectedChainPath,
        boolean selectedChainPathRootMatchesBoundRoot,
        boolean selectedChainPathTruncated,
        int selectedChainPathSize,
        String observationError
) {
    public FabricChatClefCraftResourceAssociationSnapshot {
        binding = binding == null ? Optional.empty() : binding;
    }
}
