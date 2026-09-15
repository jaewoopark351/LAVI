package lavi.minecraft.fabric.chatclef.bridge.command.result.instant;

import lavi.minecraft.command.result.instant.InstantCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import java.util.LinkedHashMap;

//20260915_kpopmodder: Project exact synchronous results only after the existing callback evidence.
public final class FabricChatClefInstantResultProjector {
    private FabricChatClefInstantResultProjector() { }

    public static FabricChatClefCommandResultPayload project(String requestId,
            FabricChatClefCommandResultDataPayload base, InstantCommandResult result) {
        if (result == null) return null;
        var values = new LinkedHashMap<String, Object>(base.toMap());
        values.put("result_reason", "instant_command_observed");
        values.put("result_fidelity", "command_callback_plus_native_result");
        values.put("instant_command", result.toMap());
        var data = FabricChatClefCommandResultDataPayload.fromMap(values);
        return switch (result.outcome()) {
            case "completed" -> FabricChatClefCommandResult.completed(requestId, "Native command result observed.", data);
            case "failed" -> FabricChatClefCommandResult.failed(requestId, "Native command reported an unsuccessful result.", data);
            default -> FabricChatClefCommandResult.unknown(requestId, "Native command returned; its complete effect was not verified.", data);
        };
    }
}
