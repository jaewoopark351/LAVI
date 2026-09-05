package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Validate the closed STOP protocol profile after correlation reservation.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class FabricChatClefStopControlProfileValidator {
    private static final Pattern EVENT_ID = Pattern.compile("[0-9a-f]{32}");
    private static final Set<String> PAYLOAD_FIELDS = Set.of(
            "request_id", "command", "source", "deadline_ms", "metadata"
    );
    private static final Set<String> INPUT_EVENT_FIELDS = Set.of(
            "source", "provider_id", "event_kind", "final", "event_id"
    );
    private static final Set<String> GENERAL_METADATA_FIELDS = Set.of(
            "request_kind", "operation", "input_event", "server_connection_generation", "target_scope"
    );
    private static final List<String> TARGET_METADATA_FIELDS = List.of(
            "target_request_id",
            "target_command_message_id",
            "target_session_id",
            "target_server_connection_generation"
    );

    private final FabricChatClefStopControlJsonFieldValidator fields;

    public FabricChatClefStopControlProfileValidator(FabricChatClefStopControlJsonFieldValidator fields) {
        this.fields = fields;
    }

    public FabricChatClefStopControlValidationDecision validate(
            FabricChatClefStopControlBaseRequest base,
            JsonNode envelope,
            Optional<FabricChatClefAcceptedSessionIdentity> acceptedIdentity,
            long nowMs
    ) {
        JsonNode payload = envelope.get("payload");
        JsonNode metadata = payload.get("metadata");
        Long deadlineMs = fields.exactPositiveLong(payload.get("deadline_ms"));
        if (!fields.exactLongEquals(envelope.get("protocol_version"), 1L)
                || !fields.exactTextEquals(envelope.get("message_type"), "command_request")
                || !fields.isNonNegativeLong(envelope.get("timestamp_ms"))
                || !fields.hasOnlyFields(payload, PAYLOAD_FIELDS)
                || !fields.exactTextEquals(payload.get("command"), "stop")
                || !fields.isNonBlankText(payload.get("source"))
                || deadlineMs == null
                || !fields.exactTextEquals(metadata.get("request_kind"), "stop_control_v1")
                || !fields.exactTextEquals(metadata.get("operation"), "stop_ai")
                || !validGeneralMetadataShape(metadata)
                || !validInputEvent(payload.get("source").textValue(), metadata.get("input_event"))) {
            return FabricChatClefStopControlValidationDecision.rejected("invalid_control_profile", null);
        }
        FabricChatClefAcceptedSessionIdentity accepted = acceptedIdentity.orElse(null);
        if (accepted == null
                || !accepted.sessionId().equals(base.identity().sessionId())
                || accepted.javaSocketGeneration() != base.javaSocketGeneration()) {
            return FabricChatClefStopControlValidationDecision.rejected("session_mismatch", null);
        }
        if (accepted.serverConnectionGeneration() != base.identity().serverConnectionGeneration()) {
            return FabricChatClefStopControlValidationDecision.rejected("server_generation_mismatch", null);
        }
        if (nowMs > deadlineMs) {
            return FabricChatClefStopControlValidationDecision.rejected("deadline_exceeded", null);
        }
        JsonNode scopeNode = metadata.get("target_scope");
        FabricChatClefStopControlTargetScope scope = scopeNode != null && scopeNode.isTextual()
                ? FabricChatClefStopControlTargetScope.fromWireValue(scopeNode.textValue())
                : null;
        if (scope == null) {
            return FabricChatClefStopControlValidationDecision.rejected("invalid_target_scope", null);
        }
        String invalidTargetFieldsMask = invalidTargetFieldsMask(metadata, scope);
        if (!"none".equals(invalidTargetFieldsMask)) {
            return FabricChatClefStopControlValidationDecision.rejected(
                    "invalid_target_fields",
                    scope,
                    invalidTargetFieldsMask
            );
        }
        String targetRequestId = null;
        String targetMessageId = null;
        String targetSessionId = null;
        Long targetGeneration = null;
        if (scope == FabricChatClefStopControlTargetScope.TRACKED_COMMAND) {
            targetRequestId = fields.exactIdentityText(metadata.get("target_request_id"));
            targetMessageId = fields.exactIdentityText(metadata.get("target_command_message_id"));
            targetSessionId = fields.exactIdentityText(metadata.get("target_session_id"));
            targetGeneration = fields.exactPositiveLong(metadata.get("target_server_connection_generation"));
            if (targetRequestId == null || targetMessageId == null
                    || targetSessionId == null || targetGeneration == null) {
                return FabricChatClefStopControlValidationDecision.rejected("invalid_target_fields", scope);
            }
        }
        return FabricChatClefStopControlValidationDecision.accepted(
                new FabricChatClefStopControlRequest(
                        base,
                        deadlineMs,
                        scope,
                        targetRequestId,
                        targetMessageId,
                        targetSessionId,
                        targetGeneration
                )
        );
    }

    private String invalidTargetFieldsMask(
            JsonNode metadata,
            FabricChatClefStopControlTargetScope scope
    ) {
        List<String> violations = new ArrayList<>();
        if (scope == FabricChatClefStopControlTargetScope.CURRENT_GLOBAL_AUTOMATION) {
            for (String field : TARGET_METADATA_FIELDS) {
                if (metadata.has(field)) {
                    violations.add("extra:" + field);
                }
            }
            return violations.isEmpty() ? "none" : String.join(",", violations);
        }
        for (String field : TARGET_METADATA_FIELDS) {
            if (!metadata.has(field)) {
                violations.add("missing:" + field);
                continue;
            }
            boolean valid = "target_server_connection_generation".equals(field)
                    ? fields.exactPositiveLong(metadata.get(field)) != null
                    : fields.exactIdentityText(metadata.get(field)) != null;
            if (!valid) {
                violations.add("invalid:" + field);
            }
        }
        return violations.isEmpty() ? "none" : String.join(",", violations);
    }

    private boolean validGeneralMetadataShape(JsonNode metadata) {
        if (metadata == null || !metadata.isObject()
                || !metadata.has("request_kind")
                || !metadata.has("operation")
                || !metadata.has("input_event")
                || !metadata.has("server_connection_generation")) {
            return false;
        }
        var names = metadata.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (!GENERAL_METADATA_FIELDS.contains(field) && !TARGET_METADATA_FIELDS.contains(field)) {
                return false;
            }
        }
        return true;
    }

    private boolean validInputEvent(String requestSource, JsonNode inputEvent) {
        if (inputEvent == null || !inputEvent.isObject()
                || !fields.hasOnlyFields(inputEvent, INPUT_EVENT_FIELDS)) {
            return false;
        }
        JsonNode source = inputEvent.get("source");
        JsonNode providerId = inputEvent.get("provider_id");
        JsonNode eventKind = inputEvent.get("event_kind");
        JsonNode finalValue = inputEvent.get("final");
        JsonNode eventId = inputEvent.get("event_id");
        return fields.isNonBlankText(source)
                && requestSource.equals(source.textValue())
                && fields.isNonBlankText(providerId)
                && fields.isNonBlankText(eventKind)
                && finalValue != null && finalValue.isBoolean() && finalValue.booleanValue()
                && eventId != null && eventId.isTextual()
                && EVENT_ID.matcher(eventId.textValue()).matches();
    }
}
