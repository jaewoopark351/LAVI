package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FabricChatClefBoundRootDetachOwnershipTest {
    @Test
    void dispatcherDetachSkipsPreexistingIdleRootCancellation() {
        IdleTask idle = new IdleTask();
        Harness harness = harness(idle, FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT);

        harness.dispatcher.handleConnectionDetached(FabricChatClefConnectionDetachedEvent.of(1L, "test"), idle);

        assertEquals("skip_preexisting_root", harness.dispatcher.lastDetachCancelAction());
        assertEquals(
                "PREEXISTING_UNCHANGED_IDLE_ROOT:no_bound_root_task",
                harness.dispatcher.lastBoundRootOwnershipForDetach()
        );
        assertFalse(harness.queue.hasActive());
    }

    @Test
    void dispatcherDetachPreservesCommandOwnedExactRootCancellationPath() {
        IdleTask idle = new IdleTask();
        Harness harness = harness(idle, FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT);

        harness.dispatcher.handleConnectionDetached(FabricChatClefConnectionDetachedEvent.of(1L, "test"), idle);

        assertEquals("cancel_owned_root", harness.dispatcher.lastDetachCancelAction());
        assertEquals(
                "COMMAND_OWNED_ROOT:same_task_instance",
                harness.dispatcher.lastBoundRootOwnershipForDetach()
        );
        assertFalse(harness.queue.hasActive());
    }

    private static Harness harness(
            IdleTask idle,
            FabricChatClefRootOwnershipClassification classification
    ) {
        FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
        TestResultSender sender = new TestResultSender();
        FabricChatClefTaskStateReader taskStateReader = new FabricChatClefTaskStateReader();
        FabricChatClefCommandQueue queue = new FabricChatClefCommandQueue();
        FabricChatClefCommandContext context = context();
        queue.offer(context);
        queue.pollForDispatch();
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
                evidence(idle)
        );
        coordinator.beginExecution(execution);
        execution.markDispatchReturned(evidence(idle), classification);
        FabricChatClefCommandDispatcher dispatcher = new FabricChatClefCommandDispatcher(
                queue,
                sender,
                coordinator,
                diagnostics,
                taskStateReader
        );
        return new Harness(queue, dispatcher);
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

    private static final class Harness {
        private final FabricChatClefCommandQueue queue;
        private final FabricChatClefCommandDispatcher dispatcher;

        private Harness(FabricChatClefCommandQueue queue, FabricChatClefCommandDispatcher dispatcher) {
            this.queue = queue;
            this.dispatcher = dispatcher;
        }
    }

    private static final class TestResultSender implements FabricChatClefCommandResultSender {
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
            return FabricChatClefCommandResultSendSubmission.accepted();
        }
    }
}
