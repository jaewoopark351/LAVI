package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.completion;

import java.util.Map;

//20260814_kpopmodder: Keep the termination completion source key isolated without changing its value.
public final class FabricChatClefTerminationCompletionSourcePayloadMap {
    private static final String COMPLETION_SOURCE = "completion_source";
    private static final String COMPLETION_SOURCE_VALUE = "altoclef_task_finished_event";

    private FabricChatClefTerminationCompletionSourcePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload) {
        payload.put(COMPLETION_SOURCE, COMPLETION_SOURCE_VALUE);
    }
}
