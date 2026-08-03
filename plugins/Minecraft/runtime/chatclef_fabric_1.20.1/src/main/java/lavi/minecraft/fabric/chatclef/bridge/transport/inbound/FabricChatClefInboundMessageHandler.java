package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

//20260804_kpopmodder: Keep Fabric bridge inbound envelope routing out of the WebSocket listener.
public final class FabricChatClefInboundMessageHandler {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefSessionGuard sessionGuard;
    private final FabricChatClefCommandRequestHandler commandRequestHandler;

    public FabricChatClefInboundMessageHandler(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefCommandRequestHandler commandRequestHandler
    ) {
        this.diagnostics = diagnostics;
        this.json = json;
        this.sessionGuard = sessionGuard;
        this.commandRequestHandler = commandRequestHandler;
    }

    public void handle(long generation, String message) {
        try {
            FabricChatClefBridgeEnvelope envelope = json.decode(message);
            if (envelope.protocolVersion != 1) {
                diagnostics.warn("ignored unsupported protocol_version=" + envelope.protocolVersion);
                return;
            }
            if ("handshake_ack".equals(envelope.messageType)) {
                sessionGuard.acceptHandshake(envelope);
                return;
            }
            if ("status_snapshot".equals(envelope.messageType)) {
                diagnostics.info("received status_snapshot");
                return;
            }
            if ("command_request".equals(envelope.messageType)) {
                commandRequestHandler.handle(envelope, generation);
                return;
            }
            if ("error".equals(envelope.messageType)) {
                diagnostics.warn("received error envelope payload=" + envelope.payload);
                return;
            }
            diagnostics.warn("ignored unsupported message_type=" + envelope.messageType);
        } catch (Exception error) {
            diagnostics.warn("message decode failed " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }
}
