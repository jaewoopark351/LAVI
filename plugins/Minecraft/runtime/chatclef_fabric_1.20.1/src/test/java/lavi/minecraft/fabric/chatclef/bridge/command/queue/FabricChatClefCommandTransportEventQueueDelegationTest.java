package lavi.minecraft.fabric.chatclef.bridge.command.queue;

//20260905_kpopmodder: Lock delegation to independent detach and result-completion channels.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefCommandTransportEventQueueDelegationTest {
    @Test
    void legacyFacadeDelegatesToIndependentTypedChannels() {
        FabricChatClefCommandTransportEventQueue queue =
                new FabricChatClefCommandTransportEventQueue();
        FabricChatClefCommandResultSendCompletion completion =
                FabricChatClefCommandResultSendCompletion.of(
                        ordinary("ordinary-a"),
                        FabricChatClefCommandResultSendOutcome.sent()
                );

        queue.enqueueConnectionDetached(41L, "socket_closed");
        queue.enqueueResultCompletion(completion);

        FabricChatClefConnectionDetachedEvent detached =
                queue.pollConnectionDetached().orElseThrow();
        assertEquals(41L, detached.connectionGeneration());
        assertEquals("socket_closed", detached.reason());
        assertSame(completion, queue.pollResultCompletion());
        assertTrue(queue.pollConnectionDetached().isEmpty());
    }

    @Test
    void clearDelegatesToBothTypedChannels() {
        FabricChatClefCommandTransportEventQueue queue =
                new FabricChatClefCommandTransportEventQueue();
        queue.enqueueConnectionDetached(42L, "shutdown");
        queue.enqueueResultCompletion(FabricChatClefCommandResultSendCompletion.of(
                ordinary("ordinary-b"),
                FabricChatClefCommandResultSendOutcome.sent()
        ));

        queue.clear();

        assertTrue(queue.pollConnectionDetached().isEmpty());
        assertNull(queue.pollResultCompletion());
    }

    private FabricChatClefCommandContext ordinary(String requestId) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = "get oak_log 1";
        request.source = "test";
        return new FabricChatClefCommandContext(request, requestId + "-message", "session-a", 41L, 7L);
    }
}
