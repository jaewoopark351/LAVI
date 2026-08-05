package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Keep command lifecycle detail fields typed until the Map edge.
public final class FabricChatClefLifecycleDetailsPayload implements FabricChatClefCommandDiagnosticDetailsPayload {
    private static final String REPLACED_ACTIVE_REQUEST_ID = "replaced_active_request_id";
    private static final String RUNTIME = "runtime";
    private static final String DECISION_REASON = "decision_reason";
    private static final String TERMINAL_SENT = "terminal_sent";
    private static final String LIFECYCLE_CLEARED = "lifecycle_cleared";
    private static final String QUEUE_ACTIVE_PRESENT = "queue_active_present";
    private static final String QUEUE_ACTIVE_REQUEST_ID = "queue_active_request_id";
    private static final String WAITING_REASON = "waiting_reason";
    private static final String CURRENT_TASK_BOUND_ROOT_MATCH_REASON = "current_task_bound_root_match_reason";
    private static final String EXCEPTION_TYPE = "exception_type";
    private static final String EXCEPTION_MESSAGE = "exception_message";

    private final Map<String, Object> details;

    private FabricChatClefLifecycleDetailsPayload(Map<String, Object> details) {
        this.details = details == null ? new HashMap<>() : new HashMap<>(details);
    }

    public static FabricChatClefLifecycleDetailsPayload replacedActive(String replacedActiveRequestId) {
        Map<String, Object> details = new HashMap<>();
        details.put(REPLACED_ACTIVE_REQUEST_ID, nullToEmpty(replacedActiveRequestId));
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    public static FabricChatClefLifecycleDetailsPayload finishCallback(
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> details = callbackCurrentTask.toMap();
        details.put(RUNTIME, runtime.toMap());
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    public static FabricChatClefLifecycleDetailsPayload terminalDecision(String decisionReason) {
        Map<String, Object> details = new HashMap<>();
        details.put(DECISION_REASON, nullToEmpty(decisionReason));
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    public static FabricChatClefLifecycleDetailsPayload terminalResult(boolean terminalSent, boolean lifecycleCleared) {
        Map<String, Object> details = new HashMap<>();
        details.put(TERMINAL_SENT, terminalSent);
        details.put(LIFECYCLE_CLEARED, lifecycleCleared);
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    public static FabricChatClefLifecycleDetailsPayload queueContextMismatch(
            boolean queueActivePresent,
            String queueActiveRequestId
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put(QUEUE_ACTIVE_PRESENT, queueActivePresent);
        details.put(QUEUE_ACTIVE_REQUEST_ID, nullToEmpty(queueActiveRequestId));
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    public static FabricChatClefLifecycleDetailsPayload waitingForTerminalCondition(
            String waitingReason,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put(WAITING_REASON, nullToEmpty(waitingReason));
        details.putAll(currentTask.toMap());
        details.put(CURRENT_TASK_BOUND_ROOT_MATCH_REASON, nullToEmpty(currentTaskBoundRootMatchReason));
        details.put(RUNTIME, runtime.toMap());
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    public static FabricChatClefLifecycleDetailsPayload exception(Throwable exception) {
        Map<String, Object> details = new HashMap<>();
        details.put(EXCEPTION_TYPE, exception == null ? "" : exception.getClass().getName());
        details.put(EXCEPTION_MESSAGE, exception == null ? "" : nullSafeMessage(exception));
        return new FabricChatClefLifecycleDetailsPayload(details);
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>(details);
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
