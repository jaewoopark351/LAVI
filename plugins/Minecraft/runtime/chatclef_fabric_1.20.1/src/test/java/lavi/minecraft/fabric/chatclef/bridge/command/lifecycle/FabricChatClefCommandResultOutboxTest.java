package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefCommandResultOutboxTest {
    @Test
    void immediateTerminalSubmissionFailureLeavesActiveOwnershipUncleared() {
        Harness harness = harness(new TestResultSender(SendMode.IMMEDIATE_FAILURE));

        boolean started = harness.outbox.sendTerminal(
                harness.execution,
                harness.execution::unknownFromPreexistingUnchangedIdleRoot
        );

        assertFalse(started);
        assertTrue(harness.queue.hasActive());
        assertFalse(harness.context.terminalSent());
    }

    @Test
    void asyncTerminalSendFailureLeavesActiveOwnershipUncleared() {
        Harness harness = harness(new TestResultSender(SendMode.ACCEPT_ASYNC));

        boolean started = harness.outbox.sendTerminal(
                harness.execution,
                harness.execution::unknownFromPreexistingUnchangedIdleRoot
        );
        harness.queue.enqueueCommandResultSendCompletion(
                FabricChatClefCommandResultSendCompletion.of(
                        harness.context,
                        FabricChatClefCommandResultSendOutcome.failed(
                                FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED,
                                "async failed"
                        )
                )
        );

        assertTrue(started);
        assertFalse(harness.outbox.drainSendCompletions(harness.execution));
        assertTrue(harness.queue.hasActive());
        assertFalse(harness.context.terminalSent());
    }

    @Test
    void retryBackoffAfterTerminalFailureLeavesActiveOwnershipUncleared() {
        Harness harness = harness(new TestResultSender(SendMode.IMMEDIATE_FAILURE));

        harness.outbox.sendTerminal(
                harness.execution,
                harness.execution::unknownFromPreexistingUnchangedIdleRoot
        );
        boolean secondAttemptStarted = harness.outbox.sendTerminal(
                harness.execution,
                harness.execution::unknownFromPreexistingUnchangedIdleRoot
        );

        assertFalse(secondAttemptStarted);
        assertTrue(harness.queue.hasActive());
        assertFalse(harness.context.terminalSent());
    }

    private static Harness harness(TestResultSender sender) {
        FabricChatClefCommandQueue queue = new FabricChatClefCommandQueue();
        FabricChatClefCommandContext context = context();
        queue.offer(context);
        queue.pollForDispatch();
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        FabricChatClefCommandResultOutbox outbox = new FabricChatClefCommandResultOutbox(
                queue,
                sender,
                new FabricChatClefBridgeDiagnostics()
        );
        return new Harness(queue, context, execution, outbox);
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-outbox";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-outbox", "session-outbox", 1L);
    }

    private enum SendMode {
        IMMEDIATE_FAILURE,
        ACCEPT_ASYNC
    }

    private static final class Harness {
        private final FabricChatClefCommandQueue queue;
        private final FabricChatClefCommandContext context;
        private final FabricChatClefCommandExecution execution;
        private final FabricChatClefCommandResultOutbox outbox;

        private Harness(
                FabricChatClefCommandQueue queue,
                FabricChatClefCommandContext context,
                FabricChatClefCommandExecution execution,
                FabricChatClefCommandResultOutbox outbox
        ) {
            this.queue = queue;
            this.context = context;
            this.execution = execution;
            this.outbox = outbox;
        }
    }

    private static final class TestResultSender implements FabricChatClefCommandResultSender {
        private final SendMode mode;

        private TestResultSender(SendMode mode) {
            this.mode = mode;
        }

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
            if (mode == SendMode.IMMEDIATE_FAILURE) {
                return FabricChatClefCommandResultSendSubmission.failed(
                        FabricChatClefCommandResultSendOutcome.failed(
                                FabricChatClefCommandResultSendStatus.NO_SOCKET,
                                "no socket"
                        )
                );
            }
            return FabricChatClefCommandResultSendSubmission.accepted();
        }
    }
}
