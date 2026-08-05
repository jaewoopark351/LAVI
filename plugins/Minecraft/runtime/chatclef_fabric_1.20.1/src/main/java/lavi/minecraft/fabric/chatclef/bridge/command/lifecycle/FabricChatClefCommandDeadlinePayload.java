package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import java.util.Map;

//20260805_kpopmodder: Keep deadline diagnostic keys centralized without changing timeout behavior.
public final class FabricChatClefCommandDeadlinePayload {
    private static final String AUTOMATION_CANCELLED = "automation_cancelled";
    private static final String TASK_MAY_STILL_BE_RUNNING = "task_may_still_be_running";
    private static final String LATE_TERMINAL_EVENT_WILL_BE_IGNORED = "late_terminal_event_will_be_ignored";

    private FabricChatClefCommandDeadlinePayload() {
    }

    public static Map<String, Object> markTaskMayStillBeRunning(Map<String, Object> payload) {
        payload.put(AUTOMATION_CANCELLED, false);
        payload.put(TASK_MAY_STILL_BE_RUNNING, true);
        payload.put(LATE_TERMINAL_EVENT_WILL_BE_IGNORED, true);
        return payload;
    }
}
