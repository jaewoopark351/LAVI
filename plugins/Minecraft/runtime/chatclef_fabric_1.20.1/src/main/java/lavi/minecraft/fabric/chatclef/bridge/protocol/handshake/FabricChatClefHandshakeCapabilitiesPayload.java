package lavi.minecraft.fabric.chatclef.bridge.protocol.handshake;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Own the Fabric ChatClef handshake capability fields in one typed payload.
final class FabricChatClefHandshakeCapabilitiesPayload {
    private final boolean fabricChatClefBridge;
    private final boolean chatClefCommandDispatch;
    private final boolean chatClefStopControlV1;

    private FabricChatClefHandshakeCapabilitiesPayload(
            boolean fabricChatClefBridge,
            boolean chatClefCommandDispatch,
            boolean chatClefStopControlV1
    ) {
        this.fabricChatClefBridge = fabricChatClefBridge;
        this.chatClefCommandDispatch = chatClefCommandDispatch;
        this.chatClefStopControlV1 = chatClefStopControlV1;
    }

    static FabricChatClefHandshakeCapabilitiesPayload phase4CommandDispatch() {
        //20260905_kpopmodder: Advertise STOP control only as the JSON boolean literal true.
        return new FabricChatClefHandshakeCapabilitiesPayload(true, true, true);
    }

    Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fabric_chatclef_bridge", fabricChatClefBridge);
        payload.put("chatclef_command_dispatch", chatClefCommandDispatch);
        payload.put("chatclef_stop_control_v1", chatClefStopControlV1);
        return payload;
    }
}
