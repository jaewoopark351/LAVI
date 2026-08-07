package lavi.minecraft.fabric.chatclef.bridge.protocol.handshake;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Own the Fabric ChatClef handshake metadata fields in one typed payload.
final class FabricChatClefHandshakeMetadataPayload {
    private final String backend;
    private final String loader;
    private final String phase;

    private FabricChatClefHandshakeMetadataPayload(String backend, String loader, String phase) {
        this.backend = backend;
        this.loader = loader;
        this.phase = phase;
    }

    static FabricChatClefHandshakeMetadataPayload phase4CommandDispatch() {
        return new FabricChatClefHandshakeMetadataPayload(
                "fabric_chatclef",
                "fabric",
                "phase_4_tick_dispatch"
        );
    }

    Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("backend", backend);
        payload.put("loader", loader);
        payload.put("phase", phase);
        return payload;
    }
}
