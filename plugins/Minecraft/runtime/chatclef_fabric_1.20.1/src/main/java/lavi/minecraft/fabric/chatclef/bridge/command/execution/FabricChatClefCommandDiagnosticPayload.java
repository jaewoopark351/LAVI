package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;

import java.util.HashMap;
import java.util.Map;

//20260803_kpopmodder: Keep command lifecycle state separate from diagnostic payload map assembly.
public final class FabricChatClefCommandDiagnosticPayload {
    private FabricChatClefCommandDiagnosticPayload() {
    }

    public static Map<String, Object> commandData(
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
            FabricChatClefCommandTerminationObservation observation
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("result_fidelity", "callback_plus_matching_user_task_event");
        payload.put("result_reason", resultReason);
        payload.put("dispatch_started_ms", dispatchStartedMs);
        payload.put("dispatch_returned", dispatchReturned);
        payload.put("dispatch_thread", dispatchThreadName);
        payload.put("normalized_command_length", normalizedCommand.length());
        payload.put("finish_callback_received", finishCallbackReceived);
        payload.put("failure_type", failureType);
        payload.put("failure_message", failureMessage);
        payload.put("ownership", context.ownershipData());
        payload.put("task_before_dispatch", taskBeforeDispatch.toMap());
        payload.put("task_after_dispatch", taskAfterDispatch.toMap());
        payload.put("terminal_task", terminalTask.toMap());
        payload.put("bound_root_task", boundRootTask.toMap());
        payload.put("task_finished_event_received", observation != null);
        payload.put(
                "task_finished_observation",
                observation == null ? new HashMap<String, Object>() : observation.toMap()
        );
        return payload;
    }

    public static Map<String, Object> diagnosticData(
            String diagnosticReason,
            FabricChatClefCommandRequest request,
            String normalizedCommand,
            long elapsedMs,
            Map<String, Object> commandData
    ) {
        Map<String, Object> payload = new HashMap<>(commandData);
        payload.put("request_command", request.command == null ? "" : request.command);
        payload.put("request_source", request.source == null ? "" : request.source);
        payload.put("normalized_command", normalizedCommand);
        payload.put("elapsed_ms", elapsedMs);
        payload.put("result_reason", diagnosticReason);
        return payload;
    }
}
