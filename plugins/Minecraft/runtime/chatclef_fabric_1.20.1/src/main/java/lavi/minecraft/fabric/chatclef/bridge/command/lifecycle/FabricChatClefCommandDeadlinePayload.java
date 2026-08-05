package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Keep deadline diagnostic keys centralized without changing timeout behavior.
public final class FabricChatClefCommandDeadlinePayload implements FabricChatClefCommandResultDataPayload {
    private static final String AUTOMATION_CANCELLED = "automation_cancelled";
    private static final String TASK_MAY_STILL_BE_RUNNING = "task_may_still_be_running";
    private static final String LATE_TERMINAL_EVENT_WILL_BE_IGNORED = "late_terminal_event_will_be_ignored";

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
        Map<String, Object> payload = new HashMap<>(basePayload.toMap());
        payload.put(AUTOMATION_CANCELLED, false);
        payload.put(TASK_MAY_STILL_BE_RUNNING, true);
        payload.put(LATE_TERMINAL_EVENT_WILL_BE_IGNORED, true);
        return payload;
    }
}
