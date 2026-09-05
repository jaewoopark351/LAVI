package lavi.minecraft.fabric.chatclef.bridge.transport.session.validation;

//20260905_kpopmodder: Lock pure ACK validation independently from mutable session coordination.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefHandshakeAckValidatorTest {
    private final FabricChatClefHandshakeAckValidator validator =
            new FabricChatClefHandshakeAckValidator();

    @Test
    void acceptsExactCorrelationAndKeepsServerAndJavaGenerationsDistinct() {
        FabricChatClefHandshakeAckValidation validation = validator.validate(
                ack("handshake-a", "session-a", 41L, true),
                7L,
                "handshake-a",
                7L
        );

        assertTrue(validation.accepted());
        assertEquals("session-a", validation.identity().sessionId());
        assertEquals(41L, validation.identity().serverConnectionGeneration());
        assertEquals(7L, validation.identity().javaSocketGeneration());
        assertEquals("handshake-a", validation.identity().handshakeCorrelationId());
    }

    @Test
    void classifiesRejectedUncorrelatedAndMalformedAcksWithoutStateMutation() {
        FabricChatClefHandshakeAckValidation rejected = validator.validate(
                ack("handshake-a", "session-a", 41L, false),
                7L,
                "handshake-a",
                7L
        );
        FabricChatClefHandshakeAckValidation uncorrelated = validator.validate(
                ack("handshake-b", "session-a", 41L, true),
                7L,
                "handshake-a",
                7L
        );
        FabricChatClefBridgeEnvelope malformedEnvelope = ack(
                "handshake-a",
                "session-a",
                41L,
                true
        );
        malformedEnvelope.payload.put("connection_generation", 4.5D);
        FabricChatClefHandshakeAckValidation malformed = validator.validate(
                malformedEnvelope,
                7L,
                "handshake-a",
                7L
        );

        assertFalse(rejected.accepted());
        assertEquals("accepted_false", rejected.rejectionCode());
        assertFalse(uncorrelated.accepted());
        assertEquals("uncorrelated", uncorrelated.rejectionCode());
        assertFalse(malformed.accepted());
        assertEquals("malformed", malformed.rejectionCode());
    }

    private FabricChatClefBridgeEnvelope ack(
            String correlationId,
            String sessionId,
            long serverGeneration,
            boolean accepted
    ) {
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.messageType = "handshake_ack";
        envelope.correlationId = correlationId;
        envelope.sessionId = sessionId;
        envelope.payload = new HashMap<>();
        envelope.payload.put("accepted", accepted);
        envelope.payload.put("session_id", sessionId);
        envelope.payload.put("connection_generation", serverGeneration);
        return envelope;
    }
}
