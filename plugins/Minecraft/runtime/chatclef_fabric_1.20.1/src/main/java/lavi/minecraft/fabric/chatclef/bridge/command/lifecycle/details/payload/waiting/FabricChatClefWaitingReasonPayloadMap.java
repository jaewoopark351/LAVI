package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting;

import java.util.Map;

//20260808_kpopmodder: Keep terminal-wait reason fields separate without changing emitted keys.
public final class FabricChatClefWaitingReasonPayloadMap {
    private static final String WAITING_REASON = "waiting_reason";

    private FabricChatClefWaitingReasonPayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, String waitingReason) {
        details.put(WAITING_REASON, waitingReason);
    }
}
