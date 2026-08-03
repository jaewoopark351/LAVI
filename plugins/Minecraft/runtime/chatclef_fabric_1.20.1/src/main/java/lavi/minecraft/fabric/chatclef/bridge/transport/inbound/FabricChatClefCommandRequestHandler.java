package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefResultEnvelopeSender;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

//20260804_kpopmodder: Keep Fabric command_request validation and queueing outside the WebSocket listener.
public final class FabricChatClefCommandRequestHandler {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefResultEnvelopeSender resultEnvelopeSender;
    private final FabricChatClefSessionGuard sessionGuard;

    public FabricChatClefCommandRequestHandler(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefResultEnvelopeSender resultEnvelopeSender,
            FabricChatClefSessionGuard sessionGuard
    ) {
        this.commandQueue = commandQueue;
        this.diagnostics = diagnostics;
        this.json = json;
        this.resultEnvelopeSender = resultEnvelopeSender;
        this.sessionGuard = sessionGuard;
    }

    public void handle(FabricChatClefBridgeEnvelope envelope, long generation) {
        if (!sessionGuard.handshakeAccepted()) {
            resultEnvelopeSender.sendCommandResult(
                    envelope.messageId,
                    envelope.sessionId,
                    generation,
                    FabricChatClefCommandResult.rejected(
                            "",
                            "not_connected",
                            "Fabric ChatClef bridge handshake has not been accepted."
                    )
            );
            return;
        }
        if (!sessionGuard.isActiveSession(envelope.sessionId)) {
            resultEnvelopeSender.sendCommandResult(
                    envelope.messageId,
                    envelope.sessionId,
                    generation,
                    FabricChatClefCommandResult.rejected(
                            "",
                            "invalid_request",
                            "Fabric ChatClef command_request session does not match active handshake."
                    )
            );
            return;
        }
        FabricChatClefCommandRequest request = json.commandRequest(envelope.payload);
        FabricChatClefCommandContext context = new FabricChatClefCommandContext(
                request,
                envelope.messageId,
                envelope.sessionId,
                generation
        );
        if (!request.isValid()) {
            resultEnvelopeSender.sendCommandResult(
                    envelope.messageId,
                    envelope.sessionId,
                    generation,
                    FabricChatClefCommandResult.rejected(
                            request.requestId,
                            "invalid_request",
                            "Fabric ChatClef command_request requires request_id and command."
                    )
            );
            return;
        }
        if (!commandQueue.offer(context)) {
            diagnostics.warn(
                    "rejected command_request rejected_by=java_command_queue request="
                            + request.requestId
                            + " source="
                            + request.source
                            + " active_request="
                            + commandQueue.activeRequestId().orElse("<pending>")
                            + " generation="
                            + generation
            );
            resultEnvelopeSender.sendCommandResult(
                    envelope.messageId,
                    envelope.sessionId,
                    generation,
                    FabricChatClefCommandResult.rejected(
                            request.requestId,
                            "invalid_request",
                            "Fabric ChatClef command already pending or active: "
                                    + commandQueue.activeRequestId().orElse("<pending>")
                    )
            );
            return;
        }
        diagnostics.info(
                "queued command request="
                        + request.requestId
                        + " source="
                        + request.source
                        + " command="
                        + request.command
                        + " generation="
                        + generation
        );
    }
}
