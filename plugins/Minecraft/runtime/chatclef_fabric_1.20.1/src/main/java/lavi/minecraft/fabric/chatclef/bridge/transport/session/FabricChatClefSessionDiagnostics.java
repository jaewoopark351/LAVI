package lavi.minecraft.fabric.chatclef.bridge.transport.session;

//20260905_kpopmodder: Render bounded handshake diagnostics without owning ACK decisions or state.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

public final class FabricChatClefSessionDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefSessionDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void duplicateAcceptedAck() {
        diagnostics.warn("ignored duplicate accepted handshake ACK");
    }

    public void rejectedAck(String rejectionCode, long javaSocketGeneration) {
        if ("accepted_false".equals(rejectionCode)) {
            diagnostics.warn("handshake rejected accepted=false");
            return;
        }
        if ("uncorrelated".equals(rejectionCode)) {
            diagnostics.warn("ignored uncorrelated handshake ACK generation=" + javaSocketGeneration);
            return;
        }
        diagnostics.warn("ignored malformed accepted handshake ACK");
    }

    public void accepted(FabricChatClefAcceptedSessionIdentity identity) {
        diagnostics.info(
                "handshake accepted session="
                        + identity.sessionId()
                        + " server_generation="
                        + identity.serverConnectionGeneration()
                        + " java_socket_generation="
                        + identity.javaSocketGeneration()
        );
    }
}
