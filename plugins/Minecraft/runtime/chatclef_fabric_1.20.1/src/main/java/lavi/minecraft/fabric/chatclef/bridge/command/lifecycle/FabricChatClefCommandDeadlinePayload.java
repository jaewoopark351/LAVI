package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.FabricChatClefCommandDeadlinePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.Map;

//20260805_kpopmodder: Preserve the deadline diagnostic wrapper without changing timeout behavior.
public final class FabricChatClefCommandDeadlinePayload implements FabricChatClefCommandResultDataPayload {
    private final FabricChatClefCommandResultDataPayload basePayload;

    private FabricChatClefCommandDeadlinePayload(FabricChatClefCommandResultDataPayload basePayload) {
        this.basePayload = basePayload == null ? FabricChatClefCommandResultDataPayload.empty() : basePayload;
    }

    public static FabricChatClefCommandResultDataPayload markTaskMayStillBeRunning(
            FabricChatClefCommandResultDataPayload payload
    ) {
        return new FabricChatClefCommandDeadlinePayload(payload);
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefCommandDeadlinePayloadMap.toMap(basePayload);
    }
}
