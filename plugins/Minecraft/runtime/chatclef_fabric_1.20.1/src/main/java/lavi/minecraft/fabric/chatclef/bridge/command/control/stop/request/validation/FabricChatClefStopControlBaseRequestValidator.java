package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Validate only the correlation identity needed before STOP dedupe reservation.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;

public final class FabricChatClefStopControlBaseRequestValidator {
    private final FabricChatClefStopControlJsonFieldValidator fields;
    private final FabricChatClefStopControlRequestFingerprint fingerprint;

    public FabricChatClefStopControlBaseRequestValidator(
            FabricChatClefStopControlJsonFieldValidator fields,
            FabricChatClefStopControlRequestFingerprint fingerprint
    ) {
        this.fields = fields;
        this.fingerprint = fingerprint;
    }

    public FabricChatClefStopControlBaseValidation validate(JsonNode envelope, long javaSocketGeneration) {
        if (envelope == null || !envelope.isObject() || javaSocketGeneration <= 0L) {
            return invalid();
        }
        JsonNode payload = envelope.get("payload");
        JsonNode metadata = payload == null ? null : payload.get("metadata");
        String messageId = fields.exactIdentityText(envelope.get("message_id"));
        String sessionId = fields.exactIdentityText(envelope.get("session_id"));
        String requestId = payload == null ? null : fields.exactIdentityText(payload.get("request_id"));
        Long serverGeneration = metadata == null
                ? null
                : fields.exactPositiveLong(metadata.get("server_connection_generation"));
        if (payload == null || !payload.isObject()
                || metadata == null || !metadata.isObject()
                || messageId == null || sessionId == null || requestId == null || serverGeneration == null) {
            return invalid();
        }
        FabricChatClefStopControlIdentity identity = new FabricChatClefStopControlIdentity(
                sessionId,
                serverGeneration,
                requestId,
                messageId
        );
        return FabricChatClefStopControlBaseValidation.valid(
                new FabricChatClefStopControlBaseRequest(
                        identity,
                        javaSocketGeneration,
                        fingerprint.compute(envelope)
                )
        );
    }

    private FabricChatClefStopControlBaseValidation invalid() {
        return FabricChatClefStopControlBaseValidation.invalid("invalid_stop_control_correlation_identity");
    }
}
