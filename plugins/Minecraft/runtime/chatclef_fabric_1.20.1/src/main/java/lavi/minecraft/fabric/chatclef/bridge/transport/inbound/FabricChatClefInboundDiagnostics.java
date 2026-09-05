package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Render bounded inbound routing diagnostics without retaining raw messages.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefInboundDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefInboundDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void unsupportedProtocol(int protocolVersion) {
        diagnostics.warn("ignored unsupported protocol_version=" + protocolVersion);
    }

    public void statusSnapshotReceived() {
        diagnostics.info("received status_snapshot");
    }

    public void errorEnvelopeReceived() {
        diagnostics.warn("received error envelope");
    }

    public void unsupportedMessageType(String messageType) {
        diagnostics.warn("ignored unsupported message_type=" + messageType);
    }

    public void decodeFailed(Exception error) {
        diagnostics.warn("message decode failed type=" + error.getClass().getSimpleName());
    }
}
