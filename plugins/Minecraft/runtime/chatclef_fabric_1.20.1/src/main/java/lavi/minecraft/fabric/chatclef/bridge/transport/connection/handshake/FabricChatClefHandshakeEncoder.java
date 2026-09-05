package lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake;

//20260905_kpopmodder: Own serialization of one handshake envelope.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;

import java.io.IOException;

public final class FabricChatClefHandshakeEncoder {
    private final FabricChatClefBridgeJson json;

    public FabricChatClefHandshakeEncoder(FabricChatClefBridgeJson json) {
        this.json = json;
    }

    public String encode(FabricChatClefBridgeEnvelope envelope) throws IOException {
        return json.encode(envelope);
    }
}
