package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefFinishCallbackObservationTest {
    @Test
    void duplicateCallbackDoesNotOverwriteFirstObservation() {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        IdleTask first = new IdleTask();
        IdleTask duplicate = new IdleTask();

        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(first);
        execution.closeExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(duplicate);

        assertTrue(execution.finishCallbackReceived());
        assertTrue(execution.finishCallbackFirstObservedBeforeDispatchReturn());
        assertSame(first, execution.firstFinishCallbackObservation().task());
        assertEquals(1, execution.finishCallbackDuplicateCount());
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-finish";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-finish", "session-finish", 1L);
    }
}
