package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Lock admission, context fencing, and queueing as separate ordinary request steps.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission.FabricChatClefCommandRequestAdmission;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission.FabricChatClefCommandRequestAdmissionDecision;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefCommandRequestFlowTest {
    @Test
    void admitsExactSessionAndSocketBeforeCreatingAndQueueingFencedContext() {
        FabricChatClefSessionGuard sessionGuard = acceptedSession();
        FabricChatClefCommandRequestAdmission admission =
                new FabricChatClefCommandRequestAdmission(sessionGuard);
        FabricChatClefBridgeEnvelope envelope = envelope("session-a");
        FabricChatClefCommandRequest request = request("request-a", "get map 1");

        FabricChatClefCommandRequestAdmissionDecision sessionDecision =
                admission.admitSession(envelope, 7L);
        FabricChatClefCommandRequestAdmissionDecision payloadDecision =
                admission.admitPayload(request);
        FabricChatClefCommandContext context = new FabricChatClefCommandContextFactory().create(
                request,
                envelope,
                sessionDecision.acceptedIdentity(),
                7L
        );
        FabricChatClefCommandQueue queue = new FabricChatClefCommandQueue();

        assertTrue(sessionDecision.accepted());
        assertTrue(payloadDecision.accepted());
        assertTrue(new FabricChatClefCommandRequestEnqueuer(queue).offer(context));
        assertSame(context, queue.peekPending().orElseThrow());
        assertEquals(41L, context.serverConnectionGeneration());
        assertEquals(7L, context.connectionGeneration());
    }

    @Test
    void rejectsWrongSessionAndInvalidPayloadBeforeQueueAdmission() {
        FabricChatClefCommandRequestAdmission admission =
                new FabricChatClefCommandRequestAdmission(acceptedSession());
        FabricChatClefCommandRequest invalid = request("", "");

        FabricChatClefCommandRequestAdmissionDecision wrongSession =
                admission.admitSession(envelope("session-b"), 7L);
        FabricChatClefCommandRequestAdmissionDecision invalidPayload =
                admission.admitPayload(invalid);

        assertFalse(wrongSession.accepted());
        assertEquals("invalid_request", wrongSession.rejectionCode());
        assertFalse(invalidPayload.accepted());
        assertEquals("invalid_request", invalidPayload.rejectionCode());
    }

    private FabricChatClefSessionGuard acceptedSession() {
        FabricChatClefSessionGuard guard = new FabricChatClefSessionGuard(
                new FabricChatClefBridgeState(),
                new FabricChatClefBridgeDiagnostics()
        );
        guard.beginHandshake("handshake-a", 7L);
        FabricChatClefBridgeEnvelope ack = envelope("session-a");
        ack.messageType = "handshake_ack";
        ack.correlationId = "handshake-a";
        ack.payload = new HashMap<>();
        ack.payload.put("accepted", true);
        ack.payload.put("session_id", "session-a");
        ack.payload.put("connection_generation", 41L);
        assertTrue(guard.acceptHandshake(ack, 7L));
        return guard;
    }

    private FabricChatClefBridgeEnvelope envelope(String sessionId) {
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.messageType = "command_request";
        envelope.messageId = "message-a";
        envelope.sessionId = sessionId;
        return envelope;
    }

    private FabricChatClefCommandRequest request(String requestId, String command) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = command;
        request.source = "test";
        return request;
    }
}
