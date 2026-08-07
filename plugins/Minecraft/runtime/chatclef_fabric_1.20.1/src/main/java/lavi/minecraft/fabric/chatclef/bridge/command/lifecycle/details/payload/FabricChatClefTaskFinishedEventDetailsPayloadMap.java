package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskFinishedObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTaskFinishedEventDetailsPayloadMap {
    private static final String TASK_FINISHED_EVENT = "task_finished_event";
    private static final String MATCHED_BOUND_ROOT_TASK = "matched_bound_root_task";
    private static final String EVENT_TASK_BOUND_ROOT_MATCH_REASON = "event_task_bound_root_match_reason";
    private static final String FINISH_CALLBACK_RECEIVED = "finish_callback_received";
    private static final String RUNTIME = "runtime";

    private FabricChatClefTaskFinishedEventDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefTaskFinishedObservationPayload taskFinishedEvent,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(TASK_FINISHED_EVENT, taskFinishedEvent.toMap());
        payload.put(MATCHED_BOUND_ROOT_TASK, matchedBoundRootTask);
        payload.putAll(eventTaskRelationship.toMap());
        payload.put(EVENT_TASK_BOUND_ROOT_MATCH_REASON, eventTaskBoundRootMatchReason);
        payload.put(FINISH_CALLBACK_RECEIVED, finishCallbackReceived);
        payload.put(RUNTIME, runtime.toMap());
        return payload;
    }
}
