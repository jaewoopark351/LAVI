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
import static org.junit.jupiter.api.Assertions.assertNull;

class FabricChatClefTaskFinishedEventAssociationTest {
    @Test
    void preexistingIdleRootLeavesTaskFinishedEvidenceUnbound() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                evidence(idle)
        );
        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(idle);
        execution.closeExecutorExecuteInvocation();
        execution.markDispatchReturned(evidence(idle), FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT);

        assertFalse(execution.hasBoundRootTask());
        assertFalse(execution.matchesBoundRootTask(idle));
        assertNull(execution.taskFinishedObservation());
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
        request.requestId = "req-event";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-event", "session-event", 1L);
    }
}
