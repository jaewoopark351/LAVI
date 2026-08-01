package lavi.minecraft.fabric.chatclef.bridge.protocol;

import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//20260801_kpopmodder: Build Fabric ChatClef bridge handshake messages only.
public final class FabricChatClefBridgeMessageFactory {
    public FabricChatClefBridgeEnvelope handshake(FabricChatClefBridgeState state) {
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.protocolVersion = 1;
        envelope.messageType = "handshake";
        envelope.messageId = "fabric-chatclef-" + UUID.randomUUID();
        envelope.sessionId = state.sessionId().orElse(null);
        envelope.timestampMs = System.currentTimeMillis();
        envelope.payload = handshakePayload();
        return envelope;
    }

    private Map<String, Object> handshakePayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("fabric_chatclef_bridge", true);
        capabilities.put("chatclef_command_dispatch", true);
        payload.put("capabilities", capabilities);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("backend", "fabric_chatclef");
        metadata.put("loader", "fabric");
        metadata.put("phase", "phase_4_tick_dispatch");
        payload.put("metadata", metadata);
        return payload;
    }
}
