package lavi.minecraft.fabric.chatclef.bridge.command.control.stop;

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionFormatter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260905_kpopmodder: Parse canonical STOP diagnostics and prove wire fields remain payload-derived.
final class FabricChatClefStopControlTransitionDiagnosticsTest {
    private static final List<String> WIRE_DATA_FIELDS = List.of(
            "target_scope",
            "target_resolution",
            "requested_target_request_id",
            "requested_target_command_message_id",
            "requested_target_session_id",
            "requested_target_server_connection_generation",
            "resolved_target_request_id",
            "resolved_target_command_message_id",
            "resolved_target_session_id",
            "resolved_target_server_connection_generation",
            "control_outcome",
            "target_state_before",
            "target_state_after",
            "control_reason",
            "executed_client_tick",
            "verified_client_tick",
            "stop_command_invoked",
            "original_result_delivery"
    );

    private final FabricChatClefStopControlTransitionFormatter formatter =
            new FabricChatClefStopControlTransitionFormatter();
    private final FabricChatClefStopControlResultFactory resultFactory =
            new FabricChatClefStopControlResultFactory();

    @Test
    void completedRejectedAndUnknownLogsCopyEveryWireAuthorityFieldExactly() {
        FabricChatClefStopControlBaseRequest base = base("control-a", "message-a");
        FabricChatClefStopControlContext completedContext = context(globalRequest(base));
        completedContext.capture(FabricChatClefOrdinaryCommandStopCapture.none(), "none");
        completedContext.markExecuted(100L, false, null);

        FabricChatClefStopControlContext unknownContext = context(globalRequest(base("control-u", "message-u")));
        FabricChatClefStopControlContext exceptionContext = context(globalRequest(base("control-e", "message-e")));
        exceptionContext.capture(FabricChatClefOrdinaryCommandStopCapture.none(), "none");
        exceptionContext.markExecuted(200L, false, null);

        List<FabricChatClefCommandResultPayload> payloads = List.of(
                resultFactory.completed(completedContext, 100L),
                resultFactory.rejected(base, "invalid_control_profile", null, null, null),
                resultFactory.rejected(base, "deadline_exceeded", null, null, null),
                resultFactory.unknown(unknownContext, "target_observation_failed", 150L),
                resultFactory.unknown(exceptionContext, "stop_command_exception", 200L)
        );

        for (FabricChatClefCommandResultPayload payload : payloads) {
            FabricChatClefStopControlBaseRequest payloadBase = baseFor(payload, base, unknownContext, exceptionContext);
            Map<String, String> fields = parse(formatter.wireResult(
                    payloadBase,
                    payload,
                    "none",
                    "pending",
                    "closed",
                    "blocked",
                    "unknown".equals(payload.toMap().get("status"))
            ));
            assertCanonicalShape(fields);
            assertWireAuthority(payload, fields);
            assertEquals("null", fields.get("diagnostic_disposition"));
        }
    }

    @Test
    void invalidTargetMaskIsDiagnosticOnlyAndRequestedQuartetStaysCanonicalNull() {
        FabricChatClefStopControlBaseRequest base = base("control-invalid", "message-invalid");
        FabricChatClefCommandResultPayload payload = resultFactory.rejected(
                base,
                "invalid_target_fields",
                FabricChatClefStopControlTargetScope.TRACKED_COMMAND,
                null,
                null
        );

        Map<String, String> fields = parse(formatter.wireResult(
                base,
                payload,
                "missing:target_request_id,invalid:target_server_connection_generation",
                "pending",
                "none",
                "unchanged",
                false
        ));

        assertWireAuthority(payload, fields);
        assertEquals(
                "missing:target_request_id,invalid:target_server_connection_generation",
                fields.get("invalid_target_fields_mask")
        );
        assertEquals("null", fields.get("requested_target_request_id"));
        assertEquals("null", fields.get("requested_target_command_message_id"));
        assertEquals("null", fields.get("requested_target_session_id"));
        assertEquals("null", fields.get("requested_target_server_connection_generation"));
    }

    @Test
    void noWireTransitionLeavesAllResultAuthorityFieldsExplicitlyNull() {
        String line = formatter.noWire(
                null,
                7L,
                "accepted-session",
                41L,
                "none",
                "invalid_stop_control_correlation_identity",
                "none",
                "unchanged",
                true
        );
        Map<String, String> fields = parse(line);

        assertCanonicalShape(fields);
        assertEquals("accepted-session", fields.get("accepted_session_id_at_validation"));
        assertEquals("41", fields.get("accepted_server_connection_generation_at_validation"));
        assertEquals("7", fields.get("java_socket_generation"));
        assertEquals("invalid_stop_control_correlation_identity", fields.get("diagnostic_disposition"));
        assertEquals("not_applicable", fields.get("control_result_delivery"));
        for (String authorityField : resultAuthorityFields()) {
            assertEquals("null", fields.get(authorityField), authorityField);
        }
    }

    @Test
    void formatterUsesCanonicalKeysAndNeverEmitsUnsafeIdentityTextRaw() {
        FabricChatClefStopControlBaseRequest unsafe = new FabricChatClefStopControlBaseRequest(
                new FabricChatClefStopControlIdentity(
                        "session with spaces",
                        41L,
                        "request\nraw_payload=forbidden",
                        "message\rtranscript=forbidden"
                ),
                7L,
                "raw websocket payload must never reach diagnostics"
        ).withAcceptedValidationIdentity("accepted session", 41L);

        String line = formatter.noWire(
                unsafe,
                7L,
                "accepted session",
                41L,
                "none",
                "duplicate_control_live",
                "closed",
                "blocked",
                false
        );

        assertFalse(line.contains("\n"));
        assertFalse(line.contains("\r"));
        assertFalse(line.contains("raw_payload=forbidden"));
        assertFalse(line.contains("transcript=forbidden"));
        assertFalse(line.contains("raw websocket payload"));
        assertCanonicalShape(parse(line));
    }

    @SuppressWarnings("unchecked")
    private void assertWireAuthority(
            FabricChatClefCommandResultPayload payload,
            Map<String, String> fields
    ) {
        Map<String, Object> payloadMap = payload.toMap();
        Map<String, Object> data = (Map<String, Object>) payloadMap.get("data");
        assertEquals(render(payloadMap.get("request_id")), fields.get("control_request_id"));
        assertEquals(render(payloadMap.get("status")), fields.get("control_status"));
        assertEquals(render(data.get("connection_generation")), fields.get("server_connection_generation"));
        assertEquals(render(data.get("java_socket_generation")), fields.get("java_socket_generation"));
        for (String field : WIRE_DATA_FIELDS) {
            assertEquals(render(data.get(field)), fields.get(field), field);
        }
    }

    private void assertCanonicalShape(Map<String, String> fields) {
        assertEquals(formatter.canonicalFieldOrder(), List.copyOf(fields.keySet()));
        assertEquals(Set.copyOf(formatter.canonicalFieldOrder()), fields.keySet());
        assertEquals("stop_control_transition", fields.get("event"));
        assertFalse(fields.containsKey("payload"));
        assertFalse(fields.containsKey("transcript"));
        assertFalse(fields.containsKey("token"));
    }

    private List<String> resultAuthorityFields() {
        return List.of(
                "target_scope",
                "target_resolution",
                "requested_target_request_id",
                "requested_target_command_message_id",
                "requested_target_session_id",
                "requested_target_server_connection_generation",
                "resolved_target_request_id",
                "resolved_target_command_message_id",
                "resolved_target_session_id",
                "resolved_target_server_connection_generation",
                "control_status",
                "control_outcome",
                "target_state_before",
                "target_state_after",
                "control_reason",
                "executed_client_tick",
                "verified_client_tick",
                "stop_command_invoked",
                "original_result_delivery"
        );
    }

    private FabricChatClefStopControlBaseRequest baseFor(
            FabricChatClefCommandResultPayload payload,
            FabricChatClefStopControlBaseRequest rejectedBase,
            FabricChatClefStopControlContext unknownContext,
            FabricChatClefStopControlContext exceptionContext
    ) {
        String requestId = String.valueOf(payload.toMap().get("request_id"));
        if (requestId.equals(unknownContext.request().identity().requestId())) {
            return unknownContext.request().base();
        }
        if (requestId.equals(exceptionContext.request().identity().requestId())) {
            return exceptionContext.request().base();
        }
        return rejectedBase;
    }

    private FabricChatClefStopControlContext context(FabricChatClefStopControlRequest request) {
        return new FabricChatClefStopControlContext(request, null);
    }

    private FabricChatClefStopControlRequest globalRequest(FabricChatClefStopControlBaseRequest base) {
        return new FabricChatClefStopControlRequest(
                base,
                Long.MAX_VALUE,
                FabricChatClefStopControlTargetScope.CURRENT_GLOBAL_AUTOMATION,
                null,
                null,
                null,
                null
        );
    }

    private FabricChatClefStopControlBaseRequest base(String requestId, String messageId) {
        return new FabricChatClefStopControlBaseRequest(
                new FabricChatClefStopControlIdentity("session-a", 41L, requestId, messageId),
                7L,
                "fingerprint"
        ).withAcceptedValidationIdentity("session-a", 41L);
    }

    private Map<String, String> parse(String line) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String atom : line.split(" ")) {
            int separator = atom.indexOf('=');
            assertTrue(separator > 0, atom);
            fields.put(
                    atom.substring(0, separator),
                    URLDecoder.decode(atom.substring(separator + 1), StandardCharsets.UTF_8)
            );
        }
        return fields;
    }

    private String render(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
