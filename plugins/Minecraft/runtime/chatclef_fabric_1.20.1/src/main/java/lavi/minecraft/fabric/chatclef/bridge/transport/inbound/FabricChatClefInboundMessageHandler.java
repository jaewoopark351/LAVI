package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Preserve inbound handling as a thin decode-and-route sequencing facade.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefInboundMessageHandler {
    private final FabricChatClefInboundEnvelopeDecoder decoder;
    private final FabricChatClefInboundMessageRouter router;
    private final FabricChatClefInboundDiagnostics diagnostics;

    public FabricChatClefInboundMessageHandler(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefCommandRequestHandler commandRequestHandler,
            FabricChatClefStopControlInboundHandler stopControlInboundHandler
    ) {
        this.decoder = new FabricChatClefInboundEnvelopeDecoder(json);
        this.diagnostics = new FabricChatClefInboundDiagnostics(diagnostics);
        this.router = new FabricChatClefInboundMessageRouter(
                decoder,
                sessionGuard,
                commandRequestHandler,
                stopControlInboundHandler,
                this.diagnostics
        );
    }

    public void handle(long generation, String message) {
        try {
            router.route(decoder.decodeRaw(message), generation);
        } catch (Exception error) {
            diagnostics.decodeFailed(error);
        }
    }
}
