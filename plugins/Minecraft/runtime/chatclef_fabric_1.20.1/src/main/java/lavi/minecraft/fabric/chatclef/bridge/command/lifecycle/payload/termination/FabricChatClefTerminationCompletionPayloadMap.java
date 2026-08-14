package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination;

import java.util.Map;

//20260814_kpopmodder: Keep termination completion-source fields separate without changing emitted keys.
public final class FabricChatClefTerminationCompletionPayloadMap {
    private static final String COMPLETION_SOURCE = "completion_source";
    private static final String COMPLETION_SOURCE_VALUE = "altoclef_task_finished_event";
    private static final String TERMINATION_KIND = "termination_kind";

    private FabricChatClefTerminationCompletionPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String terminationKind) {
        payload.put(COMPLETION_SOURCE, COMPLETION_SOURCE_VALUE);
        payload.put(TERMINATION_KIND, terminationKind);
    }
}
