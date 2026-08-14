package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep the termination task-stopped key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationTaskStoppedPayloadMap {
    private static final String TASK_STOPPED = "task_stopped";

    private FabricChatClefTerminationTaskStoppedPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean taskStopped) {
        payload.put(TASK_STOPPED, taskStopped);
    }
}
