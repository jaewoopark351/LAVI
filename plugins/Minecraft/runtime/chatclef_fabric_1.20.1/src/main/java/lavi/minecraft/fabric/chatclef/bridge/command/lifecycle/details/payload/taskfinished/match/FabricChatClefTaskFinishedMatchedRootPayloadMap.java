package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.match;

import java.util.Map;

//20260809_kpopmodder: Keep task-finished matched-root fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedMatchedRootPayloadMap {
    private static final String MATCHED_BOUND_ROOT_TASK = "matched_bound_root_task";

    private FabricChatClefTaskFinishedMatchedRootPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean matchedBoundRootTask) {
        payload.put(MATCHED_BOUND_ROOT_TASK, matchedBoundRootTask);
    }
}
