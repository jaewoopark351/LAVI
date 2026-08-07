package lavi.minecraft.fabric.chatclef.bridge.protocol.handshake;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Own the Fabric ChatClef handshake capability fields in one typed payload.
final class FabricChatClefHandshakeCapabilitiesPayload {
    private final boolean fabricChatClefBridge;
    private final boolean chatClefCommandDispatch;

    private FabricChatClefHandshakeCapabilitiesPayload(
            boolean fabricChatClefBridge,
            boolean chatClefCommandDispatch
    ) {
        this.fabricChatClefBridge = fabricChatClefBridge;
        this.chatClefCommandDispatch = chatClefCommandDispatch;
    }

    static FabricChatClefHandshakeCapabilitiesPayload phase4CommandDispatch() {
        return new FabricChatClefHandshakeCapabilitiesPayload(true, true);
    }

    Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fabric_chatclef_bridge", fabricChatClefBridge);
        payload.put("chatclef_command_dispatch", chatClefCommandDispatch);
        return payload;
    }
}
