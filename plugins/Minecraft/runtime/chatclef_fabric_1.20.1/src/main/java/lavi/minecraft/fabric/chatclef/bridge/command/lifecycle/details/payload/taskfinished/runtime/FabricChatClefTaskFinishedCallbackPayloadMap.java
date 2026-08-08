package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.runtime;

import java.util.Map;

//20260809_kpopmodder: Keep task-finished callback fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedCallbackPayloadMap {
    private static final String FINISH_CALLBACK_RECEIVED = "finish_callback_received";

    private FabricChatClefTaskFinishedCallbackPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean finishCallbackReceived) {
        payload.put(FINISH_CALLBACK_RECEIVED, finishCallbackReceived);
    }
}
