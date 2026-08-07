package lavi.minecraft.fabric.chatclef.bridge.protocol.handshake;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Keep Fabric ChatClef handshake payload fields typed until the v1 map edge.
public final class FabricChatClefHandshakePayload {
    private final FabricChatClefHandshakeCapabilitiesPayload capabilities;
    private final FabricChatClefHandshakeMetadataPayload metadata;

    private FabricChatClefHandshakePayload(
            FabricChatClefHandshakeCapabilitiesPayload capabilities,
            FabricChatClefHandshakeMetadataPayload metadata
    ) {
        this.capabilities = capabilities;
        this.metadata = metadata;
    }

    public static FabricChatClefHandshakePayload phase4CommandDispatch() {
        return new FabricChatClefHandshakePayload(
                FabricChatClefHandshakeCapabilitiesPayload.phase4CommandDispatch(),
                FabricChatClefHandshakeMetadataPayload.phase4CommandDispatch()
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("capabilities", capabilities.toMap());
        payload.put("metadata", metadata.toMap());
        return payload;
    }
}
