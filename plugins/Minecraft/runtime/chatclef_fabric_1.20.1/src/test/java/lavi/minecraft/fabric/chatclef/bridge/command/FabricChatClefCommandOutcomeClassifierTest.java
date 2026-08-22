package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminalDecision;
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

class FabricChatClefCommandOutcomeClassifierTest {
    @Test
    void stablePreexistingIdleRootEmitsUnknownNotCompleted() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);
        execution.markPreexistingIdleRootStabilityObservation(qualifiedObservation());

        FabricChatClefCommandTerminalDecision decision =
                new FabricChatClefCommandOutcomeClassifier().classify(execution);
        Map<String, Object> result = decision.result().toMap();
        Map<?, ?> data = (Map<?, ?>) result.get("data");

        assertTrue(decision.terminal());
        assertFalse((Boolean) result.get("ok"));
        assertEquals("unknown", result.get("status"));
        assertEquals("finish_callback_without_new_command_owned_root", data.get("result_reason"));
        assertEquals("callback_without_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void preexistingIdleRootWaitsForStableObservation() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = executionFor(idle);

        FabricChatClefCommandTerminalDecision decision =
                new FabricChatClefCommandOutcomeClassifier().classify(execution);

        assertFalse(decision.terminal());
        assertEquals("waiting_for_preexisting_idle_root_stability", decision.reason());
    }

    @Test
    void ownershipUnknownWithSynchronousCallbackRemainsNonterminal() {
        IdleTask idle = new IdleTask();
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(idle);
        execution.closeExecutorExecuteInvocation();
        execution.markDispatchReturned(
                FabricChatClefTaskOwnershipEvidence.empty(),
                FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN
        );

        FabricChatClefCommandTerminalDecision decision =
                new FabricChatClefCommandOutcomeClassifier().classify(execution);

        assertFalse(decision.terminal());
        assertEquals("root_ownership_unknown", decision.reason());
    }

    @Test
    void commandOwnedRootWithoutBoundTaskDoesNotUseNoRootCompletion() {
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
        execution.markFinishCallbackReceived(null);
        execution.markDispatchReturned(
                FabricChatClefTaskOwnershipEvidence.empty(),
                FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT
        );

        FabricChatClefCommandTerminalDecision decision =
                new FabricChatClefCommandOutcomeClassifier().classify(execution);

        assertFalse(decision.terminal());
        assertEquals("command_owned_root_missing_bound_task", decision.reason());
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
        request.requestId = "req-outcome";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-outcome", "session-outcome", 1L);
    }
}
