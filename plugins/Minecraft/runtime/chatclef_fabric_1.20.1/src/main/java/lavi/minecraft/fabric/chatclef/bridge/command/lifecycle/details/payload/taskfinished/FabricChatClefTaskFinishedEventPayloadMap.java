package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskFinishedObservationPayload;

import java.util.Map;

//20260808_kpopmodder: Keep task-finished event detail fields separate without changing emitted keys.
public final class FabricChatClefTaskFinishedEventPayloadMap {
    private static final String TASK_FINISHED_EVENT = "task_finished_event";

    private FabricChatClefTaskFinishedEventPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskFinishedObservationPayload taskFinishedEvent
    ) {
        payload.put(TASK_FINISHED_EVENT, taskFinishedEvent.toMap());
    }
}
