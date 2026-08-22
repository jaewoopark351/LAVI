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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void rootChangeResetsStabilityWindow() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        gate.observe(execution, evidence(idle, 2L), 1200L, 1_200_000_000L, 2L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(new IdleTask(), 3L), 1600L, 1_600_000_000L, 3L);

        assertFalse(observation.qualified());
        assertEquals("current_root_changed", observation.toMap().get("reset_reason"));
    }

    @Test
    void contextChangeResetsStabilityWindow() {
        IdleTask idle = new IdleTask();
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();
        FabricChatClefCommandExecution firstExecution = executionFor(idle);
        FabricChatClefCommandExecution secondExecution = executionFor(idle);

        gate.observe(firstExecution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        gate.observe(firstExecution, evidence(idle, 2L), 1200L, 1_200_000_000L, 2L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(secondExecution, evidence(idle, 3L), 1600L, 1_600_000_000L, 3L);

        assertFalse(observation.qualified());
        assertEquals(1, observation.toMap().get("distinct_tick_count"));
    }

    @Test
    void explicitEventResetClearsStabilityWindow() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        gate.observe(execution, evidence(idle, 2L), 1200L, 1_200_000_000L, 2L);
        gate.reset();
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(idle, 3L), 1600L, 1_600_000_000L, 3L);

        assertFalse(observation.qualified());
        assertEquals(1, observation.toMap().get("distinct_tick_count"));
    }

    @Test
    void assignmentChangeResetsStabilityWindow() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(idle, 2L, "user-root-2", 1L, false), 1200L, 1_200_000_000L, 2L);

        assertFalse(observation.qualified());
        assertEquals("assignment_or_generation_changed", observation.toMap().get("reset_reason"));
    }

    @Test
    void generationChangeResetsStabilityWindow() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(idle, 2L, "user-root-1", 2L, false), 1200L, 1_200_000_000L, 2L);

        assertFalse(observation.qualified());
        assertEquals("assignment_or_generation_changed", observation.toMap().get("reset_reason"));
    }

    @Test
    void nextIdleFlagResetsStabilityWindow() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(idle, 2L, "user-root-1", 1L, true), 1200L, 1_200_000_000L, 2L);

        assertFalse(observation.qualified());
        assertEquals("next_task_idle_flag_true", observation.toMap().get("reset_reason"));
    }

    @Test
    void stabilityObservationKeepsCompatibilityFieldsAndAliases() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        FabricChatClefPreexistingIdleRootStabilityGate gate = new FabricChatClefPreexistingIdleRootStabilityGate();

        FabricChatClefStableRequestQuiescenceObservation observation =
                gate.observe(execution, evidence(idle, 1L), 1000L, 1_000_000_000L, 1L);
        Map<String, Object> payload = observation.toMap();

        assertEquals(payload.get("observation_count"), payload.get("distinct_tick_count"));
        assertEquals(payload.get("blocked_reason"), payload.get("reset_reason"));
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
        return evidence(root, clientTick, "user-root-1", 1L, false);
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(
            Task root,
            long clientTick,
            String assignmentId,
            long generation,
            boolean nextTaskIdle
    ) {
        FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(root);
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                1000L,
                clientTick,
                "test",
                rootSnapshot,
                root == null ? "" : root.getClass().getName(),
                root == null ? "none" : Integer.toHexString(System.identityHashCode(root)),
                assignmentId,
                generation,
                true,
                nextTaskIdle,
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
