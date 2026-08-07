package lavi.minecraft.fabric.chatclef.bridge.protocol;

import lavi.minecraft.fabric.chatclef.bridge.protocol.handshake.FabricChatClefHandshakePayload;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;

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
        envelope.payload = FabricChatClefHandshakePayload.phase4CommandDispatch().toMap();
        return envelope;
    }
}
