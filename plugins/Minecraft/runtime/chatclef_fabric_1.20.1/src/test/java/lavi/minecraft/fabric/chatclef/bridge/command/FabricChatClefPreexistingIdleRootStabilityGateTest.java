package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefPreexistingIdleRootStabilityGate;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricChatClefPreexistingIdleRootStabilityGateTest {
    @Test
    void repeatedObservationsInOneClientTickDoNotQualify() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        assertFalse(gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L).qualified());
        assertFalse(gate.observe(execution, evidence(idle, 1L), 1600L, 1_600_000_000L, 1L).qualified());
        assertFalse(gate.observe(execution, evidence(idle, 1L), 1700L, 1_700_000_000L, 1L).qualified());
    }

    @Test
    void distinctTicksAndMinimumDurationQualify() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        gate.observe(execution, evidence(idle, 2L), 1200L, 1_200_000_000L, 2L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(idle, 3L), 1600L, 1_600_000_000L, 3L);

        assertTrue(observation.qualified());
    }

    private static FabricChatClefCommandExecution executionFor(IdleTask idle) {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                evidence(idle, 1L)
        );
        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(idle);
        execution.closeExecutorExecuteInvocation();
        execution.markDispatchReturned(evidence(idle, 1L), FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT);
        return execution;
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(Task root, long clientTick) {
        FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(root);
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                1000L,
                clientTick,
                "test",
                rootSnapshot,
                root == null ? "" : root.getClass().getName(),
                root == null ? "none" : Integer.toHexString(System.identityHashCode(root)),
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

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-stability";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-stability", "session-stability", 1L);
    }
}
