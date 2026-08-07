package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate deadline result Map keys without changing emitted diagnostic fields.
public final class FabricChatClefCommandDeadlinePayloadMap {
    private static final String AUTOMATION_CANCELLED = "automation_cancelled";
    private static final String TASK_MAY_STILL_BE_RUNNING = "task_may_still_be_running";
    private static final String LATE_TERMINAL_EVENT_WILL_BE_IGNORED = "late_terminal_event_will_be_ignored";

    private FabricChatClefCommandDeadlinePayloadMap() {
    }

    public static Map<String, Object> toMap(FabricChatClefCommandResultDataPayload basePayload) {
        FabricChatClefCommandResultDataPayload normalizedBasePayload =
                basePayload == null ? FabricChatClefCommandResultDataPayload.empty() : basePayload;
        Map<String, Object> payload = new HashMap<>(normalizedBasePayload.toMap());
        payload.put(AUTOMATION_CANCELLED, false);
        payload.put(TASK_MAY_STILL_BE_RUNNING, true);
        payload.put(LATE_TERMINAL_EVENT_WILL_BE_IGNORED, true);
        return payload;
    }
}
