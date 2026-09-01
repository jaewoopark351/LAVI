package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required;

import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalScopeBinding;

import java.util.Map;

import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.unavailableIfBlank;

//20260901_kpopmodder: Own required command and source identity fields.
public final class FabricChatClefCraftResourceTerminalIdentityRequiredFields {
    private FabricChatClefCraftResourceTerminalIdentityRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            CraftResourceTerminalSnapshot snapshot,
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            String sourceEventName) {
        fields.put("behavior_effect", "none");
        fields.put("sourceEventName", unavailableIfBlank(sourceEventName));
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put("commandRequestId", snapshot.key().commandRequestId());
        fields.put("commandCorrelationId", snapshot.key().commandCorrelationId());
        fields.put("commandSessionId", snapshot.key().commandSessionId());
        fields.put(
                "commandConnectionGeneration",
                snapshot.key().commandConnectionGeneration()
        );
        fields.put("rootAssignmentId", snapshot.key().rootAssignmentId());
        fields.put("rootTaskClass", binding.rootTaskClass());
        fields.put("rootGeneration", binding.scopeKey().rootGeneration());
        fields.put(
                "boundRootTaskInstanceId",
                binding.scopeKey().boundRootTaskInstanceId()
        );
    }
}
