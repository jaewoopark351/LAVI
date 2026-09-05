package lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake;

//20260905_kpopmodder: Own handshake-send failure state, diagnostics, and reconnect request.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;

public final class FabricChatClefHandshakeFailureHandler {
    private final FabricChatClefBridgeState bridgeState;
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefHandshakeFailureHandler(
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.bridgeState = bridgeState;
        this.diagnostics = diagnostics;
    }

    public void handle(Exception error, Runnable reconnectAction) {
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        bridgeState.markFailed(message);
        diagnostics.warn("handshake send failed " + message);
        reconnectAction.run();
    }
}
