package lavi.minecraft.fabric.chatclef.bridge.transport.session.validation;

//20260905_kpopmodder: Validate handshake ACK values without logging or mutating connection state.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

import java.util.Map;

public final class FabricChatClefHandshakeAckValidator {
    public FabricChatClefHandshakeAckValidation validate(
            FabricChatClefBridgeEnvelope envelope,
            long javaSocketGeneration,
            String expectedHandshakeMessageId,
            long expectedJavaSocketGeneration
    ) {
        Object accepted = envelope.payload.get("accepted");
        if (!Boolean.TRUE.equals(accepted)) {
            return FabricChatClefHandshakeAckValidation.rejected("accepted_false");
        }
        if (javaSocketGeneration <= 0L
                || javaSocketGeneration != expectedJavaSocketGeneration
                || expectedHandshakeMessageId.isBlank()
                || !expectedHandshakeMessageId.equals(envelope.correlationId)) {
            return FabricChatClefHandshakeAckValidation.rejected("uncorrelated");
        }
        String sessionId = strictStringPayload(envelope.payload, "session_id");
        Long serverConnectionGeneration = strictPositiveLongPayload(
                envelope.payload,
                "connection_generation"
        );
        if (sessionId == null
                || serverConnectionGeneration == null
                || (envelope.sessionId != null && !sessionId.equals(envelope.sessionId))) {
            return FabricChatClefHandshakeAckValidation.rejected("malformed");
        }
        return FabricChatClefHandshakeAckValidation.accepted(
                new FabricChatClefAcceptedSessionIdentity(
                        sessionId,
                        serverConnectionGeneration,
                        javaSocketGeneration,
                        expectedHandshakeMessageId
                )
        );
    }

    private String strictStringPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String text)) {
            return null;
        }
        return text.isBlank() ? null : text;
    }

    private Long strictPositiveLongPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)) {
            return null;
        }
        long number = ((Number) value).longValue();
        return number > 0L ? number : null;
    }
}
