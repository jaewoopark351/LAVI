package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep the termination task-present key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationTaskPresentPayloadMap {
    private static final String TASK_PRESENT = "task_present";

    private FabricChatClefTerminationTaskPresentPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean taskPresent) {
        payload.put(TASK_PRESENT, taskPresent);
    }
}
