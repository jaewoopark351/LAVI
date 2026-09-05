package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Route STOP candidates before typed ordinary inbound envelope handling.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefCommandRequestHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefStopControlInboundHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.io.IOException;

public final class FabricChatClefInboundMessageRouter {
    private final FabricChatClefInboundEnvelopeDecoder decoder;
    private final FabricChatClefSessionGuard sessionGuard;
    private final FabricChatClefCommandRequestHandler commandRequestHandler;
    private final FabricChatClefStopControlInboundHandler stopControlInboundHandler;
    private final FabricChatClefInboundDiagnostics diagnostics;

    public FabricChatClefInboundMessageRouter(
            FabricChatClefInboundEnvelopeDecoder decoder,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefCommandRequestHandler commandRequestHandler,
            FabricChatClefStopControlInboundHandler stopControlInboundHandler,
            FabricChatClefInboundDiagnostics diagnostics
    ) {
        this.decoder = decoder;
        this.sessionGuard = sessionGuard;
        this.commandRequestHandler = commandRequestHandler;
        this.stopControlInboundHandler = stopControlInboundHandler;
        this.diagnostics = diagnostics;
    }

    public void route(JsonNode rawEnvelope, long generation) throws IOException {
        if (stopControlInboundHandler != null && stopControlInboundHandler.isCandidate(rawEnvelope)) {
            stopControlInboundHandler.handle(rawEnvelope, generation);
            return;
        }
        FabricChatClefBridgeEnvelope envelope = decoder.decodeTyped(rawEnvelope);
        if (envelope.protocolVersion != 1) {
            diagnostics.unsupportedProtocol(envelope.protocolVersion);
            return;
        }
        if ("handshake_ack".equals(envelope.messageType)) {
            sessionGuard.acceptHandshake(envelope, generation);
            return;
        }
        if ("status_snapshot".equals(envelope.messageType)) {
            diagnostics.statusSnapshotReceived();
            return;
        }
        if ("command_request".equals(envelope.messageType)) {
            commandRequestHandler.handle(envelope, generation);
            return;
        }
        if ("error".equals(envelope.messageType)) {
            diagnostics.errorEnvelopeReceived();
            return;
        }
        diagnostics.unsupportedMessageType(envelope.messageType);
    }
}
