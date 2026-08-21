package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefBoundRootDetachOwnershipTest {
    @Test
    void preexistingIdleRootDoesNotMatchDetachCancellationOwnership() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle, FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT);

        assertFalse(execution.hasBoundRootTask());
        assertFalse(execution.matchesBoundRootTask(idle));
    }

    @Test
    void commandOwnedRootPreservesDetachCancellationOwnership() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle, FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);

        assertTrue(execution.hasBoundRootTask());
        assertTrue(execution.matchesBoundRootTask(idle));
    }

    private static FabricChatClefCommandExecution executionFor(
            IdleTask idle,
            FabricChatClefRootOwnershipClassification classification
    ) {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                evidence(idle)
        );
        execution.markDispatchReturned(evidence(idle), classification);
        return execution;
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(Task root) {
        FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(root);
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                1000L,
                1L,
                "test",
                rootSnapshot,
                root.getClass().getName(),
                Integer.toHexString(System.identityHashCode(root)),
                "user-root-1",
                1L,
                true,
                false,
                false,
                "",
                "",
                false,
                ""
        );
        return FabricChatClefTaskOwnershipEvidence.of(root, rootSnapshot, ownership, 1000L, 1000L, 1L, "test");
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-detach";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-detach", "session-detach", 1L);
    }
}
