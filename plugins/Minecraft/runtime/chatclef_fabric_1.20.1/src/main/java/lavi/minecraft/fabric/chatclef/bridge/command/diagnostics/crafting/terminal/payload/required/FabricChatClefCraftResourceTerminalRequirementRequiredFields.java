package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required;

import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress.CraftResourceRequirementProgressSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;

import java.util.Map;
import java.util.Optional;

import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.value;

//20260901_kpopmodder: Own required quantity-contract fields with fail-closed absence values.
public final class FabricChatClefCraftResourceTerminalRequirementRequiredFields {
    private FabricChatClefCraftResourceTerminalRequirementRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            Optional<IronPickaxeAcquisitionScopeBinding> scopeBinding,
            Optional<CraftResourceRequirementProgressSnapshot> progressSnapshot) {
        if (scopeBinding.isEmpty()) {
            fields.put("requestedItem", "UNAVAILABLE_SCOPE_BINDING_NOT_ACTIVE");
            fields.put("requestedCount", "UNAVAILABLE_SCOPE_BINDING_NOT_ACTIVE");
            fields.put("currentItemCountAtStart", "UNAVAILABLE_SCOPE_BINDING_NOT_ACTIVE");
            fields.put("currentItemCountAtTerminal", "UNAVAILABLE_NO_TERMINAL_INVENTORY_SNAPSHOT");
            fields.put("targetItemCount", "UNAVAILABLE_SCOPE_BINDING_NOT_ACTIVE");
            if (progressSnapshot.isPresent()) {
                CraftResourceRequirementProgressSnapshot progress = progressSnapshot.get();
                fields.put(
                        "requirementTransitionCount",
                        progress.requirementTransitionCount()
                );
            } else {
                fields.put(
                        "requirementTransitionCount",
                        "UNAVAILABLE_SCOPE_BINDING_NOT_ACTIVE"
                );
            }
            return;
        }
        CraftResourceRequirementDecision requirement = scopeBinding.get().requirement();
        fields.put("requestedItem", requirement.requestedItem());
        fields.put("requestedCount", value(requirement.requestedCount()));
        fields.put("currentItemCountAtStart", value(requirement.currentItemCount()));
        fields.put("currentItemCountAtTerminal", "UNAVAILABLE_NO_TERMINAL_INVENTORY_SNAPSHOT");
        fields.put("targetItemCount", value(requirement.targetItemCount()));
        if (progressSnapshot.isPresent()) {
            CraftResourceRequirementProgressSnapshot progress = progressSnapshot.get();
            fields.put(
                    "requirementTransitionCount",
                    progress.requirementTransitionCount()
            );
        } else {
            fields.put(
                    "requirementTransitionCount",
                    "UNAVAILABLE_REQUIREMENT_PROGRESS_LEDGER_NOT_ACTIVE"
            );
        }
    }
}
