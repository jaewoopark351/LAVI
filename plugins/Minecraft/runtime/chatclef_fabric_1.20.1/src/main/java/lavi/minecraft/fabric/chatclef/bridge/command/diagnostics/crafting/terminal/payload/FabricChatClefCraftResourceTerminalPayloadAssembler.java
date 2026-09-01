package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload;

import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalScopeBinding;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.optional.FabricChatClefCraftResourceTerminalOptionalFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.FabricChatClefCraftResourceTerminalAssociationRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.FabricChatClefCraftResourceTerminalCoverageRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.FabricChatClefCraftResourceTerminalIdentityRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.FabricChatClefCraftResourceTerminalLifecycleRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.FabricChatClefCraftResourceTerminalMismatchRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.FabricChatClefCraftResourceTerminalRequirementRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.target.FabricChatClefCraftResourceTerminalFailureRequiredFields;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.target.FabricChatClefCraftResourceTerminalTargetAttemptRequiredFields;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

//20260901_kpopmodder: Assemble required and optional terminal responsibilities once.
public final class FabricChatClefCraftResourceTerminalPayloadAssembler {
    public FabricChatClefCraftResourceTerminalPayload assemble(
            CraftResourceTerminalDecision decision,
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            FabricChatClefCraftResourceTerminalEvidence evidence,
            String sourceEventName) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(evidence, "evidence");
        CraftResourceTerminalSnapshot snapshot = decision.snapshot();
        Map<String, Object> required = new LinkedHashMap<>();
        FabricChatClefCraftResourceTerminalIdentityRequiredFields.append(
                required,
                snapshot,
                binding,
                sourceEventName
        );
        FabricChatClefCraftResourceTerminalLifecycleRequiredFields.append(
                required,
                decision,
                snapshot
        );
        FabricChatClefCraftResourceTerminalRequirementRequiredFields.append(
                required,
                evidence.scopeBinding(),
                evidence.requirementProgressSnapshot()
        );
        FabricChatClefCraftResourceTerminalTargetAttemptRequiredFields.append(
                required,
                evidence.targetSnapshot()
        );
        FabricChatClefCraftResourceTerminalFailureRequiredFields.append(
                required,
                evidence.failureSnapshot()
        );
        FabricChatClefCraftResourceTerminalAssociationRequiredFields.append(
                required,
                evidence.associationSnapshot()
        );
        FabricChatClefCraftResourceTerminalMismatchRequiredFields.append(
                required,
                evidence.commandMismatchSnapshot()
        );
        FabricChatClefCraftResourceTerminalCoverageRequiredFields.append(required);
        return new FabricChatClefCraftResourceTerminalPayload(
                required,
                FabricChatClefCraftResourceTerminalOptionalFields.assemble(
                        binding,
                        evidence,
                        snapshot
                )
        );
    }
}
