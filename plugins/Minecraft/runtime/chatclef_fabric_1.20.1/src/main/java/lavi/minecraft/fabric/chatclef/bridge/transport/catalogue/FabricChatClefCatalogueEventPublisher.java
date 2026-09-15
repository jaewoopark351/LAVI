package lavi.minecraft.fabric.chatclef.bridge.transport.catalogue;

import lavi.minecraft.fabric.chatclef.bridge.catalogue.FabricChatClefCatalogueSnapshotStore;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

//20260915_kpopmodder: Publish each changed immutable catalogue once per accepted socket/session.
public final class FabricChatClefCatalogueEventPublisher {
    private final FabricChatClefWebSocketConnectionState connection;
    private final FabricChatClefSessionGuard session;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private String key = "";
    private int attempts;
    private boolean inFlight;
    private boolean sent;
    private long nextAttemptMs;

    public FabricChatClefCatalogueEventPublisher(FabricChatClefWebSocketConnectionState connection,
            FabricChatClefSessionGuard session, FabricChatClefBridgeJson json,
            FabricChatClefBridgeDiagnostics diagnostics) {
        this.connection = connection;
        this.session = session;
        this.json = json;
        this.diagnostics = diagnostics;
    }

    public synchronized void publishIfChanged() {
        var identity = session.acceptedIdentity().orElse(null);
        var socket = connection.webSocket();
        if (identity == null || socket == null || identity.javaSocketGeneration() != connection.activeConnectionGeneration()) return;
        var snapshot = FabricChatClefCatalogueSnapshotStore.current();
        String candidateKey = identity.sessionId() + ":" + identity.javaSocketGeneration() + ":" + snapshot.revision();
        if (!candidateKey.equals(key)) {
            key = candidateKey;
            attempts = 0;
            sent = false;
            inFlight = false;
            nextAttemptMs = 0;
        }
        if (sent || inFlight || attempts >= 3 || System.currentTimeMillis() < nextAttemptMs) return;
        var envelope = new FabricChatClefBridgeEnvelope();
        envelope.protocolVersion = 1;
        envelope.messageType = "event";
        envelope.messageId = "fabric-catalogue-" + UUID.randomUUID();
        envelope.sessionId = identity.sessionId();
        envelope.timestampMs = System.currentTimeMillis();
        envelope.payload = Map.of("event_type", "korean_command_catalogue_v1",
                "korean_command_catalogue_v1", snapshot.wireValue());
        attempts++;
        inFlight = true;
        try {
            String encoded = json.encode(envelope);
            if (encoded.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) {
                attempts = 3;
                inFlight = false;
                log(false, "catalogue_publish result=rejected reason=envelope_limit generation=" + identity.javaSocketGeneration());
                return;
            }
            socket.sendText(encoded, true).whenComplete((ignored, error) -> completed(candidateKey, error));
        } catch (Exception error) {
            completed(candidateKey, error);
        }
    }

    private synchronized void completed(String candidateKey, Throwable error) {
        if (!candidateKey.equals(key)) return;
        inFlight = false;
        sent = error == null;
        nextAttemptMs = System.currentTimeMillis() + 1000;
        if (sent) log(true, "catalogue_publish result=sent attempts=" + attempts);
        else log(false, "catalogue_publish result=failed attempts=" + attempts + " exhausted=" + (attempts >= 3));
    }

    private void log(boolean success, String message) {
        try { if (success) diagnostics.info(message); else diagnostics.warn(message); }
        catch (RuntimeException ignored) { /* logging cannot choose publication or retry state */ }
    }
}
