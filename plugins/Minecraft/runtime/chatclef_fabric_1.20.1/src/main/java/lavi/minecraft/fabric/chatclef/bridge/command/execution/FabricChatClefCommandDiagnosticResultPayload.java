package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Isolate command diagnostic result payload fields without changing emitted keys.
final class FabricChatClefCommandDiagnosticResultPayload implements FabricChatClefCommandResultDataPayload {
    private static final String REQUEST_COMMAND = "request_command";
    private static final String REQUEST_SOURCE = "request_source";
    private static final String NORMALIZED_COMMAND = "normalized_command";
    private static final String ELAPSED_MS = "elapsed_ms";
    private static final String RESULT_REASON = "result_reason";

    private final String diagnosticReason;
    private final FabricChatClefCommandRequest request;
    private final String normalizedCommand;
    private final long elapsedMs;
    private final FabricChatClefCommandResultDataPayload commandData;

    FabricChatClefCommandDiagnosticResultPayload(
            String diagnosticReason,
            FabricChatClefCommandRequest request,
            String normalizedCommand,
            long elapsedMs,
            FabricChatClefCommandResultDataPayload commandData
    ) {
        this.diagnosticReason = diagnosticReason;
        this.request = request;
        this.normalizedCommand = normalizedCommand;
        this.elapsedMs = elapsedMs;
        this.commandData = commandData == null ? FabricChatClefCommandResultDataPayload.empty() : commandData;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>(commandData.toMap());
        payload.put(REQUEST_COMMAND, request.command == null ? "" : request.command);
        payload.put(REQUEST_SOURCE, request.source == null ? "" : request.source);
        payload.put(NORMALIZED_COMMAND, normalizedCommand);
        payload.put(ELAPSED_MS, elapsedMs);
        payload.put(RESULT_REASON, diagnosticReason);
        return payload;
    }
}
