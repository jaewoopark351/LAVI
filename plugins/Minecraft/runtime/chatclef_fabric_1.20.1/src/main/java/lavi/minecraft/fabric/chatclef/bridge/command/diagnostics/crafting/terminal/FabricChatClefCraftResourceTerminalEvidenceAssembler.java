package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.progress.FabricChatClefCraftResourceRequirementProgressDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;

//20260901_kpopmodder: Read existing bounded ledgers without recomputing gameplay facts.
final class FabricChatClefCraftResourceTerminalEvidenceAssembler {
    FabricChatClefCraftResourceTerminalEvidence assemble(
            FabricChatClefCraftResourceTerminalScopeBinding binding
    ) {
        CraftResourceTargetDiagnosticsRegistry targetRegistry =
                FabricChatClefCraftResourceTargetScopeDiagnostics.registry();
        return new FabricChatClefCraftResourceTerminalEvidence(
                IronPickaxeAcquisitionScopeDiagnostics.activeBinding(binding.scopeKey()),
                FabricChatClefCraftResourceRequirementProgressDiagnostics.snapshot(
                        binding.scopeKey()
                ),
                FabricChatClefCraftResourceAssociationScopeDiagnostics.snapshot(
                        binding.scopeKey()
                ),
                targetRegistry.currentSnapshot(binding.scopeKey()),
                targetRegistry.failureSnapshot(binding.scopeKey()),
                targetRegistry.commandMismatchSnapshot(binding.scopeKey()),
                targetRegistry.snapshot()
        );
    }
}
