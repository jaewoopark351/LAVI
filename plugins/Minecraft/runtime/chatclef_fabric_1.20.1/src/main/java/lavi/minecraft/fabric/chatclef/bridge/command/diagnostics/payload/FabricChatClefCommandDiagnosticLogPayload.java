package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Split command diagnostic log payload assembly from log emission without changing keys.
public final class FabricChatClefCommandDiagnosticLogPayload {
    private static final String EVENT = "event";
    private static final String DETAILS = "details";

    private FabricChatClefCommandDiagnosticLogPayload() {
    }

    public static Map<String, Object> execution(
            String event,
            FabricChatClefCommandExecution execution
    ) {
        return execution(event, execution, FabricChatClefCommandDiagnosticDetailsMapPayload.empty());
    }

    public static Map<String, Object> execution(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        return execution(event, execution, detailsMap(details));
    }

    private static Map<String, Object> execution(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        Map<String, Object> payload = execution == null
                ? new HashMap<>()
                : execution.diagnosticPayload(event).toMap();
        addCommonFields(payload, event, details);
        return payload;
    }

    public static Map<String, Object> context(
            String event,
            FabricChatClefCommandContext context
    ) {
        return context(event, context, FabricChatClefCommandDiagnosticDetailsMapPayload.empty());
    }

    public static Map<String, Object> context(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        return context(event, context, detailsMap(details));
    }

    private static Map<String, Object> context(
            String event,
            FabricChatClefCommandContext context,
            Map<String, Object> details
    ) {
        Map<String, Object> payload = context == null
                ? new HashMap<>()
                : context.ownershipPayload().toMap();
        addCommonFields(payload, event, details);
        return payload;
    }

    private static Map<String, Object> detailsMap(FabricChatClefCommandDiagnosticDetailsPayload details) {
        return details == null ? emptyDetails() : details.toMap();
    }

    private static Map<String, Object> emptyDetails() {
        return new HashMap<>();
    }

    private static void addCommonFields(
            Map<String, Object> payload,
            String event,
            Map<String, Object> details
    ) {
        payload.put(EVENT, event);
        payload.put(DETAILS, details == null ? emptyDetails() : details);
    }
}
