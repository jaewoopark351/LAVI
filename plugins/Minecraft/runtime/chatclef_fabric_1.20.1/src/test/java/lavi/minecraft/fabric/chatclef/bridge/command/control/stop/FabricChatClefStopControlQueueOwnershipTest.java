package lavi.minecraft.fabric.chatclef.bridge.command.control.stop;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260905_kpopmodder: Lock one-entry STOP ownership, ordinary offer/poll fencing, and bounded dedupe.
class FabricChatClefStopControlQueueOwnershipTest {
    @Test
    void admittedStopBlocksOrdinaryOfferAndPollUntilExactRelease() {
        FabricChatClefStopControlAdmissionBarrier barrier = new FabricChatClefStopControlAdmissionBarrier();
        FabricChatClefCommandQueue ordinaryQueue = new FabricChatClefCommandQueue(barrier);
        FabricChatClefStopControlQueue stopQueue = new FabricChatClefStopControlQueue(barrier);
        FabricChatClefCommandContext original = ordinary("ordinary-a");
        assertTrue(ordinaryQueue.offer(original));

        FabricChatClefStopControlContext stop = stopQueue.offer(stop("stop-a", "message-a")).orElseThrow();
        FabricChatClefOrdinaryCommandStopCapture capture = ordinaryQueue.captureForUserStop();

        assertTrue(capture.pending());
        assertSame(original, capture.context());
        assertTrue(barrier.blocksOrdinary());
        assertFalse(ordinaryQueue.offer(ordinary("ordinary-b")));
        assertTrue(ordinaryQueue.pollForDispatch().isEmpty());
        assertTrue(stopQueue.offer(stop("stop-b", "message-b")).isEmpty());
        assertTrue(stopQueue.completeAndRelease(stop));
        assertFalse(barrier.blocksOrdinary());
        assertSame(original, ordinaryQueue.pollForDispatch().orElseThrow());
    }

    @Test
    void admissionReturnsTheExactContextCommittedWhileHoldingTheBarrier() {
        FabricChatClefStopControlAdmissionBarrier barrier = new FabricChatClefStopControlAdmissionBarrier();
        FabricChatClefStopControlQueue stopQueue = new FabricChatClefStopControlQueue(barrier);

        FabricChatClefStopControlContext committed = stopQueue.offer(stop("stop-a", "message-a")).orElseThrow();

        assertSame(committed, stopQueue.active().orElseThrow());
        assertSame(committed, barrier.inspectOwned(committed.barrierToken(), () -> committed).orElseThrow());
    }

    @Test
    void nonEvictingRegistryRejectsIdentityFourThousandNinetySeven() {
        FabricChatClefStopControlDedupeRegistry registry = new FabricChatClefStopControlDedupeRegistry();
        registry.selectAcceptedGeneration(41L);
        for (int index = 0; index < FabricChatClefStopControlDedupeRegistry.MAX_IDENTITIES; index++) {
            FabricChatClefStopControlDedupeDecision decision = registry.reserve(
                    new FabricChatClefStopControlIdentity(
                            "session-a",
                            41L,
                            "request-" + index,
                            "message-" + index
                    ),
                    "fingerprint-" + index
            );
            assertEquals(FabricChatClefStopControlDedupeDecision.RESERVED, decision);
        }

        FabricChatClefStopControlIdentity overflow = new FabricChatClefStopControlIdentity(
                "session-a",
                41L,
                "request-overflow",
                "message-overflow"
        );
        assertEquals(
                FabricChatClefStopControlDedupeDecision.CAPACITY_EXHAUSTED,
                registry.reserve(overflow, "overflow")
        );
        assertEquals(FabricChatClefStopControlDedupeRegistry.MAX_IDENTITIES, registry.size());
    }

    @Test
    void replayNeverCreatesCompetingReservation() {
        FabricChatClefStopControlDedupeRegistry registry = new FabricChatClefStopControlDedupeRegistry();
        FabricChatClefStopControlIdentity identity =
                new FabricChatClefStopControlIdentity("session-a", 41L, "request-a", "message-a");
        registry.selectAcceptedGeneration(41L);

        assertEquals(FabricChatClefStopControlDedupeDecision.RESERVED, registry.reserve(identity, "same"));
        assertEquals(FabricChatClefStopControlDedupeDecision.DUPLICATE_LIVE, registry.reserve(identity, "same"));
        assertEquals(
                FabricChatClefStopControlDedupeDecision.DUPLICATE_PAYLOAD_MISMATCH,
                registry.reserve(identity, "different")
        );
        assertTrue(registry.markTerminal(identity));
        assertEquals(
                FabricChatClefStopControlDedupeDecision.DUPLICATE_TOMBSTONED,
                registry.reserve(identity, "same")
        );
        assertEquals(1, registry.size());
    }

    private FabricChatClefCommandContext ordinary(String requestId) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = "get oak_log 1";
        request.source = "test";
        return new FabricChatClefCommandContext(request, requestId + "-message", "session-a", 41L, 7L);
    }

    private FabricChatClefStopControlRequest stop(String requestId, String messageId) {
        FabricChatClefStopControlIdentity identity =
                new FabricChatClefStopControlIdentity("session-a", 41L, requestId, messageId);
        FabricChatClefStopControlBaseRequest base =
                new FabricChatClefStopControlBaseRequest(identity, 7L, requestId);
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
}
