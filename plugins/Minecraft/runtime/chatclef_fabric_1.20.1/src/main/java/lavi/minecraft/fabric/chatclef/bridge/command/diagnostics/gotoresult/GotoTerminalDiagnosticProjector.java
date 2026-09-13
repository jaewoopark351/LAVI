package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.gotoresult;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
//#if MC == 12001
import lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask;
import lavi.minecraft.task.movement.gotoresult.model.GotoTaskResultSource;
//#endif

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//20260913_kpopmodder: Project an allowlist after the existing result factory has decided.
public final class GotoTerminalDiagnosticProjector {
    private static final Pattern XYZ = Pattern.compile(
            "^@?goto\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)(?:\\s+(overworld|nether|end))?$");

    public Map<String, Object> project(
            FabricChatClefCommandExecution execution, FabricChatClefCommandResultPayload result
    ) {
        String command = execution.normalizedCommand();
        if (command == null || !(command.equals("@goto") || command.startsWith("@goto ")
                || command.equals("goto") || command.startsWith("goto "))) {
            return Map.of();
        }
        Map<String, Object> payload = result.toMap();
        Map<?, ?> data = payload.get("data") instanceof Map<?, ?> typed ? typed : Map.of();
        Map<?, ?> terminal = data.get("goto_terminal") instanceof Map<?, ?> typed ? typed : Map.of();
        GotoTerminalTaskSnapshot task = taskSnapshot(execution);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", "GOTO_TERMINAL_RESULT_OBSERVED");
        fields.put("owner", "FabricChatClefCommandTerminalResultDispatcher");
        fields.put("boundary", "existing_terminal_factory_return");
        fields.put("request_id", execution.requestId());
        fields.put("correlation_id", execution.context().correlationId());
        fields.put("session_id", execution.context().sessionId());
        fields.put("server_connection_generation", execution.context().serverConnectionGeneration());
        fields.put("java_socket_generation", execution.context().connectionGeneration());
        fields.put("task_owner", task.owner());
        fields.put("task_identity", task.identity());
        fields.put("operation_id", terminal.containsKey("operation_id") ? scalar(terminal.get("operation_id"))
                : task.arrived().equals("UNAVAILABLE") ? "UNAVAILABLE" : task.identity());
        fields.put("operation_identity_scope", terminal.isEmpty() ? "task_identity_in_request_session" : "command_binding_uuid");
        fields.put("bound_root_matched", task.boundRootMatched());
        fields.put("termination_kind", task.terminationKind());
        fields.put("owner_arrived", task.arrived());
        fields.put("owner_failure_reason", task.failureReason());
        fields.put("user_stop_bound", execution.userStopBound());
        fields.put("root_ownership", execution.rootOwnershipClassification().name());
        fields.put("payload_request_id", scalar(payload.get("request_id")));
        fields.put("status", scalar(payload.get("status")));
        fields.put("ok", scalar(payload.get("ok")));
        fields.put("result_reason", scalar(data.get("result_reason")));
        fields.put("result_fidelity", scalar(data.get("result_fidelity")));
        fields.put("goal_satisfied", scalar(terminal.isEmpty() ? data.get("goal_satisfied") : terminal.get("goal_satisfied")));
        Matcher xyz = XYZ.matcher(command.length() <= 128 ? command : "");
        fields.put("request_shape", xyz.matches() ? "XYZ" : "OTHER_OR_UNAVAILABLE");
        if (xyz.matches()) {
            fields.put("target_x", xyz.group(1));
            fields.put("target_y", xyz.group(2));
            fields.put("target_z", xyz.group(3));
            fields.put("requested_dimension", xyz.group(4) == null ? "UNSPECIFIED" : xyz.group(4));
        }
        return java.util.Collections.unmodifiableMap(fields);
    }

    private GotoTerminalTaskSnapshot taskSnapshot(FabricChatClefCommandExecution execution) {
        FabricChatClefCommandTerminationObservation observation = execution.taskFinishedObservation();
        Task task = observation == null ? null : observation.task();
        String arrived = "UNAVAILABLE";
        String failure = "UNAVAILABLE";
        //#if MC == 12001
        if (task instanceof PreparedGotoTask prepared) {
            // Pure stored-value reads; never call isFinished(), goals, inventory or cleanup here.
            arrived = Boolean.toString(prepared.arrived());
            var reason = prepared.failureReason();
            failure = reason == null ? "NONE" : reason.name();
        } else if (task instanceof GotoTaskResultSource source && source.gotoTerminal() != null) {
            arrived = Boolean.toString("ARRIVED".equals(source.gotoTerminal().outcome()));
            failure = source.gotoTerminal().failureReason();
        }
        //#endif
        return new GotoTerminalTaskSnapshot(
                task == null ? "UNAVAILABLE" : task.getClass().getName(),
                task == null ? "UNAVAILABLE" : Integer.toHexString(System.identityHashCode(task)),
                arrived, failure,
                observation != null && execution.matchesBoundRootTask(observation),
                observation == null ? "UNAVAILABLE" : observation.terminationKind()
        );
    }

    private Object scalar(Object value) {
        return value instanceof String || value instanceof Boolean || value instanceof Number
                ? value : "UNAVAILABLE";
    }
}
