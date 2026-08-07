package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskFinishedObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized command lifecycle Map serialization without changing emitted diagnostic fields.
public final class FabricChatClefCommandLifecyclePayloadMap {
    private static final String RESULT_FIDELITY = "result_fidelity";
    private static final String RESULT_FIDELITY_VALUE = "callback_plus_matching_user_task_event";
    private static final String RESULT_REASON = "result_reason";
    private static final String DISPATCH_STARTED_MS = "dispatch_started_ms";
    private static final String DISPATCH_RETURNED = "dispatch_returned";
    private static final String DISPATCH_THREAD = "dispatch_thread";
    private static final String NORMALIZED_COMMAND_LENGTH = "normalized_command_length";
    private static final String FINISH_CALLBACK_RECEIVED = "finish_callback_received";
    private static final String FAILURE_TYPE = "failure_type";
    private static final String FAILURE_MESSAGE = "failure_message";
    private static final String OWNERSHIP = "ownership";
    private static final String TASK_BEFORE_DISPATCH = "task_before_dispatch";
    private static final String TASK_AFTER_DISPATCH = "task_after_dispatch";
    private static final String TERMINAL_TASK = "terminal_task";
    private static final String BOUND_ROOT_TASK = "bound_root_task";
    private static final String TASK_FINISHED_EVENT_RECEIVED = "task_finished_event_received";
    private static final String TASK_FINISHED_OBSERVATION = "task_finished_observation";

    private FabricChatClefCommandLifecyclePayloadMap() {
    }

    public static Map<String, Object> toMap(
            String resultReason,
            long dispatchStartedMs,
            boolean dispatchReturned,
            String dispatchThreadName,
            String normalizedCommand,
            boolean finishCallbackReceived,
            String failureType,
            String failureMessage,
            FabricChatClefCommandContext context,
            FabricChatClefTaskSnapshot taskBeforeDispatch,
            FabricChatClefTaskSnapshot taskAfterDispatch,
            FabricChatClefTaskSnapshot terminalTask,
            FabricChatClefTaskSnapshot boundRootTask,
            FabricChatClefTaskFinishedObservationPayload taskFinishedObservation
    ) {
        FabricChatClefTaskFinishedObservationPayload normalizedTaskFinishedObservation =
                FabricChatClefTaskFinishedObservationPayload.orEmpty(taskFinishedObservation);
        Map<String, Object> payload = new HashMap<>();
        payload.put(RESULT_FIDELITY, RESULT_FIDELITY_VALUE);
        payload.put(RESULT_REASON, resultReason);
        payload.put(DISPATCH_STARTED_MS, dispatchStartedMs);
        payload.put(DISPATCH_RETURNED, dispatchReturned);
        payload.put(DISPATCH_THREAD, dispatchThreadName);
        payload.put(NORMALIZED_COMMAND_LENGTH, normalizedCommand.length());
        payload.put(FINISH_CALLBACK_RECEIVED, finishCallbackReceived);
        payload.put(FAILURE_TYPE, failureType);
        payload.put(FAILURE_MESSAGE, failureMessage);
        payload.put(OWNERSHIP, context.ownershipPayload().toMap());
        payload.put(TASK_BEFORE_DISPATCH, taskBeforeDispatch.toMap());
        payload.put(TASK_AFTER_DISPATCH, taskAfterDispatch.toMap());
        payload.put(TERMINAL_TASK, terminalTask.toMap());
        payload.put(BOUND_ROOT_TASK, boundRootTask.toMap());
        payload.put(TASK_FINISHED_EVENT_RECEIVED, normalizedTaskFinishedObservation.received());
        payload.put(TASK_FINISHED_OBSERVATION, normalizedTaskFinishedObservation.toMap());
        return payload;
    }
}
