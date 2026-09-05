package lavi.minecraft.fabric.chatclef.bridge.command.control.stop;

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseValidation;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlCandidateDetector;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequestValidator;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlValidationDecision;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.handshake.FabricChatClefHandshakePayload;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260905_kpopmodder: Lock literal capability, partial demux, and two-generation validation semantics.
class FabricChatClefStopControlProtocolValidationTest {
    private final FabricChatClefBridgeJson json = new FabricChatClefBridgeJson();
    private final FabricChatClefStopControlCandidateDetector detector =
            new FabricChatClefStopControlCandidateDetector();
    private final FabricChatClefStopControlRequestValidator validator =
            new FabricChatClefStopControlRequestValidator();

    @Test
    void advertisesStopCapabilityAsLiteralBooleanTrue() {
        Map<String, Object> payload = FabricChatClefHandshakePayload.phase4CommandDispatch().toMap();
        Map<?, ?> capabilities = (Map<?, ?>) payload.get("capabilities");

        Object capability = capabilities.get("chatclef_stop_control_v1");

        assertSame(Boolean.TRUE, capability);
    }

    @Test
    void partialStopDiscriminatorIsClaimedAndRejectedWithoutOrdinaryFallthrough() throws Exception {
        JsonNode partial = json.decodeTree(requestJson(
                "stop",
                "stop_control_v1",
                "wrong_operation",
                "current_global_automation",
                ""
        ));
        JsonNode ordinaryStop = json.decodeTree("""
                {
                  "protocol_version":1,
                  "message_type":"command_request",
                  "message_id":"ordinary-message",
                  "session_id":"session-a",
                  "timestamp_ms":1,
                  "payload":{
                    "request_id":"ordinary-request",
                    "command":"stop",
                    "source":"direct_typed",
                    "deadline_ms":9999999999999,
                    "metadata":{}
                  }
                }
                """);

        assertTrue(detector.isCandidate(partial));
        assertFalse(detector.isCandidate(ordinaryStop));
        FabricChatClefStopControlBaseValidation base = validator.validateBase(partial, 7L);
        assertTrue(base.valid());
        FabricChatClefStopControlValidationDecision decision = validator.validateProfile(
                base.request(),
                partial,
                Optional.of(new FabricChatClefAcceptedSessionIdentity("session-a", 41L, 7L, "hs")),
                1L
        );
        assertFalse(decision.accepted());
        assertEquals("invalid_control_profile", decision.rejectionReason());
    }

    @Test
    void acceptsDistinctServerAndJavaSocketGenerationsButRejectsWrongServerGeneration() throws Exception {
        JsonNode request = json.decodeTree(requestJson(
                "stop",
                "stop_control_v1",
                "stop_ai",
                "current_global_automation",
                ""
        ));
        FabricChatClefStopControlBaseValidation base = validator.validateBase(request, 7L);

        FabricChatClefStopControlValidationDecision accepted = validator.validateProfile(
                base.request(),
                request,
                Optional.of(new FabricChatClefAcceptedSessionIdentity("session-a", 41L, 7L, "hs")),
                1L
        );
        FabricChatClefStopControlValidationDecision wrongServer = validator.validateProfile(
                base.request(),
                request,
                Optional.of(new FabricChatClefAcceptedSessionIdentity("session-a", 42L, 7L, "hs")),
                1L
        );

        assertTrue(accepted.accepted());
        assertEquals(41L, accepted.request().identity().serverConnectionGeneration());
        assertEquals(7L, accepted.request().javaSocketGeneration());
        assertFalse(wrongServer.accepted());
        assertEquals("server_generation_mismatch", wrongServer.rejectionReason());
    }

    @Test
    void invalidTargetDiagnosticsExposeOnlyDeterministicFieldNames() throws Exception {
        JsonNode trackedMissing = json.decodeTree(requestJson(
                "stop",
                "stop_control_v1",
                "stop_ai",
                "tracked_command",
                ""
        ));
        JsonNode globalExtra = json.decodeTree(requestJson(
                "stop",
                "stop_control_v1",
                "stop_ai",
                "current_global_automation",
                ",\"target_request_id\":\"raw-value-must-not-be-copied\""
        ));
        Optional<FabricChatClefAcceptedSessionIdentity> accepted = Optional.of(
                new FabricChatClefAcceptedSessionIdentity("session-a", 41L, 7L, "hs")
        );

        FabricChatClefStopControlValidationDecision missing = validator.validateProfile(
                validator.validateBase(trackedMissing, 7L).request(),
                trackedMissing,
                accepted,
                1L
        );
        FabricChatClefStopControlValidationDecision extra = validator.validateProfile(
                validator.validateBase(globalExtra, 7L).request(),
                globalExtra,
                accepted,
                1L
        );

        assertEquals("invalid_target_fields", missing.rejectionReason());
        assertEquals(
                "missing:target_request_id,missing:target_command_message_id,"
                        + "missing:target_session_id,missing:target_server_connection_generation",
                missing.invalidTargetFieldsMask()
        );
        assertEquals("invalid_target_fields", extra.rejectionReason());
        assertEquals("extra:target_request_id", extra.invalidTargetFieldsMask());
        assertFalse(extra.invalidTargetFieldsMask().contains("raw-value"));
        String fingerprint = validator.validateBase(globalExtra, 7L).request().fingerprint();
        assertEquals(64, fingerprint.length());
        assertFalse(fingerprint.contains("raw-value"));
    }

    @Test
    void acceptedHandshakeCannotBeOverwrittenByDuplicateAck() {
        FabricChatClefSessionGuard guard = new FabricChatClefSessionGuard(
                new FabricChatClefBridgeState(),
                new FabricChatClefBridgeDiagnostics()
        );
        guard.beginHandshake("handshake-a", 7L);

        assertTrue(guard.acceptHandshake(ack("handshake-a", "session-a", 41L), 7L));
        assertFalse(guard.acceptHandshake(ack("handshake-a", "session-b", 99L), 7L));

        FabricChatClefAcceptedSessionIdentity accepted = guard.acceptedIdentity().orElseThrow();
        assertEquals("session-a", accepted.sessionId());
        assertEquals(41L, accepted.serverConnectionGeneration());
        assertEquals(7L, accepted.javaSocketGeneration());
    }

    private FabricChatClefBridgeEnvelope ack(
            String correlationId,
            String sessionId,
            long serverGeneration
    ) {
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.messageType = "handshake_ack";
        envelope.correlationId = correlationId;
        envelope.sessionId = sessionId;
        envelope.payload = new HashMap<>();
        envelope.payload.put("accepted", true);
        envelope.payload.put("session_id", sessionId);
        envelope.payload.put("connection_generation", serverGeneration);
        return envelope;
    }

    private String requestJson(
            String command,
            String requestKind,
            String operation,
            String targetScope,
            String targetFields
    ) {
        return """
                {
                  "protocol_version":1,
                  "message_type":"command_request",
                  "message_id":"stop-message",
                  "session_id":"session-a",
                  "timestamp_ms":1,
                  "payload":{
                    "request_id":"stop-request",
                    "command":"%s",
                    "source":"lavi_chat",
                    "deadline_ms":9999999999999,
                    "metadata":{
                      "request_kind":"%s",
                      "operation":"%s",
                      "input_event":{
                        "source":"lavi_chat",
                        "provider_id":"local_chat",
                        "event_kind":"final_text",
                        "final":true,
                        "event_id":"0123456789abcdef0123456789abcdef"
                      },
                      "server_connection_generation":41,
                      "target_scope":"%s"%s
                    }
                  }
                }
                """.formatted(command, requestKind, operation, targetScope, targetFields);
    }
}
