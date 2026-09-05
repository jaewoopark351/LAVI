package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Build the exact original-command cancelled/user_stop_requested terminal.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricChatClefOriginalCancellationResultFactory {
    public FabricChatClefCommandResultPayload create(FabricChatClefCommandContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("result_reason", "user_stop_requested");
        data.put("connection_generation", context.serverConnectionGeneration());
        data.put("java_socket_generation", context.connectionGeneration());
        return FabricChatClefCommandResultPayload.of(
                context.requestId(),
                FabricChatClefCommandResultStatus.CANCELLED,
                null,
                "Fabric ChatClef command was cancelled by an explicit user stop request.",
                Collections.unmodifiableMap(data)
        );
    }
}
