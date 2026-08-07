package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Keep TaskFinishedEvent lifecycle detail fields typed until the Map edge.
public final class FabricChatClefTaskFinishedEventDetailsPayload implements FabricChatClefCommandDiagnosticDetailsPayload {
    private static final String TASK_FINISHED_EVENT = "task_finished_event";
    private static final String MATCHED_BOUND_ROOT_TASK = "matched_bound_root_task";
    private static final String EVENT_TASK_BOUND_ROOT_MATCH_REASON = "event_task_bound_root_match_reason";
    private static final String FINISH_CALLBACK_RECEIVED = "finish_callback_received";
    private static final String RUNTIME = "runtime";

    private final FabricChatClefCommandTerminationObservation observation;
    private final boolean matchedBoundRootTask;
    private final FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship;
    private final String eventTaskBoundRootMatchReason;
    private final boolean finishCallbackReceived;
    private final FabricChatClefTaskRuntimeObservationPayload runtime;

    private FabricChatClefTaskFinishedEventDetailsPayload(
            FabricChatClefCommandTerminationObservation observation,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        this.observation = observation;
        this.matchedBoundRootTask = matchedBoundRootTask;
        this.eventTaskRelationship = eventTaskRelationship;
        this.eventTaskBoundRootMatchReason = eventTaskBoundRootMatchReason;
        this.finishCallbackReceived = finishCallbackReceived;
        this.runtime = runtime;
    }

    public static FabricChatClefTaskFinishedEventDetailsPayload of(
            FabricChatClefCommandTerminationObservation observation,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        return new FabricChatClefTaskFinishedEventDetailsPayload(
                observation,
                matchedBoundRootTask,
                eventTaskRelationship,
                eventTaskBoundRootMatchReason,
                finishCallbackReceived,
                runtime
        );
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put(TASK_FINISHED_EVENT, observation.toMap());
        payload.put(MATCHED_BOUND_ROOT_TASK, matchedBoundRootTask);
        payload.putAll(eventTaskRelationship.toMap());
        payload.put(EVENT_TASK_BOUND_ROOT_MATCH_REASON, eventTaskBoundRootMatchReason);
        payload.put(FINISH_CALLBACK_RECEIVED, finishCallbackReceived);
        payload.put(RUNTIME, runtime.toMap());
        return payload;
    }
}
