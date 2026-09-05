package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Inspect the canonical STOP result status without mutating delivery state.

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.Map;

public final class FabricChatClefStopControlResultStatusInspector {
    public boolean isUnknown(FabricChatClefCommandResultPayload payload) {
        Map<String, Object> map = payload == null ? Map.of() : payload.toMap();
        return "unknown".equals(map.get("status"));
    }
}
