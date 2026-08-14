package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event;

import java.util.Map;

//20260814_kpopmodder: Keep the termination task-absence reason key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationTaskAbsenceReasonPayloadMap {
    private static final String TASK_ABSENCE_REASON = "task_absence_reason";

    private FabricChatClefTerminationTaskAbsenceReasonPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String taskAbsenceReason) {
        payload.put(TASK_ABSENCE_REASON, taskAbsenceReason);
    }
}
