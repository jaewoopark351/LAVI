package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationLedgerSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress.CraftResourceRequirementProgressSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistrySnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureAggregateSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command.CraftResourceCommandMismatchSnapshot;

import java.util.Optional;

//20260901_kpopmodder: Hold one read-only evidence assembly before exact-key diagnostic retirement.
public record FabricChatClefCraftResourceTerminalEvidence(
        Optional<IronPickaxeAcquisitionScopeBinding> scopeBinding,
        Optional<CraftResourceRequirementProgressSnapshot> requirementProgressSnapshot,
        Optional<CraftResourceAssociationLedgerSnapshot> associationSnapshot,
        Optional<CraftResourceTargetAttemptSnapshot> targetSnapshot,
        Optional<CraftResourceFailureAggregateSnapshot> failureSnapshot,
        Optional<CraftResourceCommandMismatchSnapshot> commandMismatchSnapshot,
        CraftResourceTargetDiagnosticsRegistrySnapshot targetRegistrySnapshot
) {
    public FabricChatClefCraftResourceTerminalEvidence {
        scopeBinding = scopeBinding == null ? Optional.empty() : scopeBinding;
        requirementProgressSnapshot = requirementProgressSnapshot == null
                ? Optional.empty()
                : requirementProgressSnapshot;
        associationSnapshot = associationSnapshot == null
                ? Optional.empty()
                : associationSnapshot;
        targetSnapshot = targetSnapshot == null ? Optional.empty() : targetSnapshot;
        failureSnapshot = failureSnapshot == null ? Optional.empty() : failureSnapshot;
        commandMismatchSnapshot = commandMismatchSnapshot == null
                ? Optional.empty()
                : commandMismatchSnapshot;
    }
}
