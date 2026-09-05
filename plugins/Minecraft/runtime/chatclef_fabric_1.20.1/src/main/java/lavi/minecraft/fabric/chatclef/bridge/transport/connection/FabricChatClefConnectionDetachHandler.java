package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Own current-socket close and error detachment handling.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.WebSocket;

public final class FabricChatClefConnectionDetachHandler {
    private final FabricChatClefConnectionCurrentSocketGuard currentSocketGuard;
    private final FabricChatClefConnectionDetachCommit detachCommit;
    private final FabricChatClefConnectionDetachNotification detachNotification;
    private final FabricChatClefConnectionDetachDiagnostics detachDiagnostics;
    private final FabricChatClefConnectionReconnectCoordinator reconnectCoordinator;

    public FabricChatClefConnectionDetachHandler(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefWebSocketConnectionState connectionState,
            FabricChatClefConnectionReconnectCoordinator reconnectCoordinator
    ) {
        this.currentSocketGuard = new FabricChatClefConnectionCurrentSocketGuard(connectionState);
        this.detachCommit = new FabricChatClefConnectionDetachCommit(connectionState);
        this.detachNotification = new FabricChatClefConnectionDetachNotification(
                commandQueue,
                sessionGuard,
                bridgeState
        );
        this.detachDiagnostics = new FabricChatClefConnectionDetachDiagnostics(diagnostics);
        this.reconnectCoordinator = reconnectCoordinator;
    }

    public void onClose(
            WebSocket webSocket,
            int statusCode,
            String reason,
            Runnable connectAction
    ) {
        FabricChatClefConnectionDetachAdmission admission = currentSocketGuard.evaluate(webSocket);
        if (!admission.isCurrentSocket()) {
            detachDiagnostics.staleClose(statusCode, reason);
            return;
        }
        detachCommit.detachCurrent();
        detachNotification.closed(admission.generation(), statusCode, reason);
        detachDiagnostics.closed(admission.generation(), statusCode, reason);
        reconnectCoordinator.schedule(connectAction);
    }

    public void onError(
            WebSocket webSocket,
            Throwable error,
            Runnable connectAction
    ) {
        FabricChatClefConnectionDetachAdmission admission = currentSocketGuard.evaluate(webSocket);
        if (!admission.isCurrentSocket()) {
            detachDiagnostics.staleError(error);
            return;
        }
        detachCommit.detachCurrent();
        String message = detachDiagnostics.errorMessage(error);
        detachNotification.failed(admission.generation(), message);
        detachDiagnostics.failed(admission.generation(), message);
        reconnectCoordinator.schedule(connectAction);
    }
}
