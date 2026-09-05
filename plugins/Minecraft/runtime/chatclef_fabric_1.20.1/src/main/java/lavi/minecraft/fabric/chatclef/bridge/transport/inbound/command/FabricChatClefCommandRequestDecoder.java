package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Decode only the command_request payload into its typed request model.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;

import java.util.Map;

public final class FabricChatClefCommandRequestDecoder {
    private final FabricChatClefBridgeJson json;

    public FabricChatClefCommandRequestDecoder(FabricChatClefBridgeJson json) {
        this.json = json;
    }

    public FabricChatClefCommandRequest decode(Map<String, Object> payload) {
        return json.commandRequest(payload);
    }
}
