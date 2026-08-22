package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefTaskFinishedEventAssociationTest {
    @Test
    void ownershipUnknownUnboundEventIsAuditOnlyAndNotAttachedThroughCoordinatorTick() {
        FabricChatClefCommandContext context = context("req-unknown-event");
        TestResultSender sender = new TestResultSender();
        CoordinatorHarness harness = coordinator(sender);
        FabricChatClefCommandLifecycleCoordinator coordinator = harness.coordinator;
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        IdleTask eventTask = new IdleTask();

        coordinator.beginExecution(execution);
        execution.openExecutorExecuteInvocation();
        coordinator.markCommandFinish(execution, eventTask);
        execution.closeExecutorExecuteInvocation();
        coordinator.markDispatchReturned(execution, FabricChatClefTaskOwnershipEvidence.empty());
        harness.observer.enqueueForTest(observation(eventTask));

        coordinator.onEndClientTick(Optional.of(context));

        assertNull(execution.taskFinishedObservation());
        assertTrue(coordinator.hasActiveExecution(context));
        assertFalse(sender.terminalSendRequested);
    }

    @Test
    void preexistingIdleRootObserverEventIsAuditOnlyAndResetsStabilityThroughCoordinatorTick() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandContext context = context("req-preexisting-event");
        TestResultSender sender = new TestResultSender();
        CoordinatorHarness harness = coordinator(sender);
        FabricChatClefCommandLifecycleCoordinator coordinator = harness.coordinator;
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                "@deposit diamond 2",
                evidence(idle, 1L)
        );

        coordinator.beginExecution(execution);
        execution.openExecutorExecuteInvocation();
        coordinator.markCommandFinish(execution, idle);
        execution.closeExecutorExecuteInvocation();
        coordinator.markDispatchReturned(execution, evidence(idle, 1L));
        execution.markPreexistingIdleRootStabilityObservation(qualifiedObservation());
        harness.observer.enqueueForTest(observation(new IdleTask()));

        coordinator.onEndClientTick(Optional.of(context));

        assertNull(execution.taskFinishedObservation());
        assertFalse(execution.preexistingIdleRootStabilityQualified());
        assertTrue(coordinator.hasActiveExecution(context));
        assertFalse(sender.terminalSendRequested);
    }

    private static FabricChatClefCommandTerminationObservation observation(Task task) {
        return FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                new TaskFinishedEvent(0.25, task)
        );
    }

    private static CoordinatorHarness coordinator(TestResultSender sender) {
        FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
        FabricChatClefTaskStateReader taskStateReader = new FabricChatClefTaskStateReader();
        FabricChatClefUserTaskFinishedObserver observer =
                new FabricChatClefUserTaskFinishedObserver(diagnostics, taskStateReader);
        FabricChatClefCommandLifecycleCoordinator coordinator = new FabricChatClefCommandLifecycleCoordinator(
                observer,
                new FabricChatClefCommandOutcomeClassifier(),
                new FabricChatClefCommandResultOutbox(new FabricChatClefCommandQueue(), sender, diagnostics),
                sender,
                diagnostics,
                taskStateReader
        );
        return new CoordinatorHarness(coordinator, observer);
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(Task root, long clientTick) {
        FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(root);
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                1000L,
                clientTick,
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
        return FabricChatClefTaskOwnershipEvidence.of(root, rootSnapshot, ownership, 1000L, 1000L, clientTick, "test");
    }

    private static FabricChatClefStableRequestQuiescenceObservation qualifiedObservation() {
        return FabricChatClefStableRequestQuiescenceObservation.of(
                true,
                "none",
                3,
                1000L,
                1600L,
                1L,
                3L,
                600L,
                600L,
                0L,
                true,
                false,
                "OBSERVED_NEUTRAL_ROOT_STABLE",
                "test",
                "test",
                3,
                500L,
                1000L
        );
    }

    private static FabricChatClefCommandContext context(String requestId) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-" + requestId, "session-event", 1L);
    }

    private static final class CoordinatorHarness {
        private final FabricChatClefCommandLifecycleCoordinator coordinator;
        private final FabricChatClefUserTaskFinishedObserver observer;

        private CoordinatorHarness(
                FabricChatClefCommandLifecycleCoordinator coordinator,
                FabricChatClefUserTaskFinishedObserver observer
        ) {
            this.coordinator = coordinator;
            this.observer = observer;
        }
    }

    private static final class TestResultSender implements FabricChatClefCommandResultSender {
        private boolean terminalSendRequested;

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
            terminalSendRequested = true;
            return FabricChatClefCommandResultSendSubmission.accepted();
        }
    }
}
