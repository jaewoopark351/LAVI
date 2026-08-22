package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefFinishCallbackLifecycleBoundaryTest {
    @Test
    void firstCallbackAfterDispatchReturnIsNotUpgradedByDuplicate() {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        IdleTask first = new IdleTask();
        IdleTask duplicate = new IdleTask();

        execution.markDispatchReturned(
                FabricChatClefTaskOwnershipEvidence.empty(),
                FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE
        );
        execution.markFinishCallbackReceived(first);
        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(duplicate);
        execution.closeExecutorExecuteInvocation();

        assertFalse(execution.finishCallbackFirstObservedBeforeDispatchReturn());
        assertSame(first, execution.firstFinishCallbackObservation().task());
        assertEquals(1, execution.finishCallbackDuplicateCount());
    }

    @Test
    void duplicateCallbackDoesNotStartSecondTerminalSend() {
        FabricChatClefCommandContext context = context();
        FabricChatClefCommandQueue queue = new FabricChatClefCommandQueue();
        queue.offer(context);
        queue.pollForDispatch();
        TestResultSender sender = new TestResultSender();
        FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
        FabricChatClefTaskStateReader taskStateReader = new FabricChatClefTaskStateReader();
        FabricChatClefUserTaskFinishedObserver observer =
                new FabricChatClefUserTaskFinishedObserver(diagnostics, taskStateReader);
        FabricChatClefCommandLifecycleCoordinator coordinator = new FabricChatClefCommandLifecycleCoordinator(
                observer,
                new FabricChatClefCommandOutcomeClassifier(),
                new FabricChatClefCommandResultOutbox(queue, sender, diagnostics),
                sender,
                diagnostics,
                taskStateReader
        );
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );

        coordinator.beginExecution(execution);
        execution.markDispatchReturned(
                FabricChatClefTaskOwnershipEvidence.empty(),
                FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE
        );
        coordinator.markCommandFinish(execution, null);
        coordinator.markCommandFinish(execution, null);

        assertEquals(1, sender.terminalSendCount);
        assertTrue(context.terminalSendInFlight());
        assertTrue(queue.hasActive());
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-finish";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-finish", "session-finish", 1L);
    }

    private static final class TestResultSender implements FabricChatClefCommandResultSender {
        private int terminalSendCount;

        @Override
        public FabricChatClefCommandResultSendSubmission sendCommandResult(
                FabricChatClefCommandContext context,
                FabricChatClefCommandResultPayload payload
        ) {
            return FabricChatClefCommandResultSendSubmission.accepted();
        }

        @Override
        public FabricChatClefCommandResultSendSubmission sendTerminalCommandResult(
                FabricChatClefCommandContext context,
                FabricChatClefCommandResultPayload payload
        ) {
            terminalSendCount++;
            return FabricChatClefCommandResultSendSubmission.accepted();
        }
    }
}
