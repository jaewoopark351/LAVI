package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Publish an admitted detach to command, session, and bridge-state owners.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefConnectionDetachNotification {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefSessionGuard sessionGuard;
    private final FabricChatClefBridgeState bridgeState;

    public FabricChatClefConnectionDetachNotification(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefBridgeState bridgeState
    ) {
        this.commandQueue = commandQueue;
        this.sessionGuard = sessionGuard;
        this.bridgeState = bridgeState;
    }

    public void closed(long generation, int statusCode, String reason) {
        notifyOwners(generation, "websocket_closed");
        bridgeState.markDisconnected("closed status=" + statusCode + " reason=" + reason);
    }

    public void failed(long generation, String message) {
        notifyOwners(generation, "websocket_error");
        bridgeState.markFailed(message);
    }

    private void notifyOwners(long generation, String reason) {
        commandQueue.enqueueConnectionDetached(generation, reason);
        sessionGuard.markConnectionDetached(generation);
    }
}
