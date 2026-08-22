package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasks.movement.IdleTask;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricChatClefCommandResultFidelityTest {
    @Test
    void runningResultUsesDispatchStartedOnlyFidelity() {
        FabricChatClefCommandExecution execution = execution();

        Map<?, ?> data = data(execution.runningResult().toMap());

        assertEquals("dispatch_started", data.get("result_reason"));
        assertEquals("dispatch_started_only", data.get("result_fidelity"));
    }

    @Test
    void noUserTaskResultUsesCallbackWithoutUserTaskFidelity() {
        FabricChatClefCommandExecution execution = execution();

        Map<?, ?> data = data(execution.completedWithoutUserTask().toMap());

        assertEquals("callback_completed_without_user_task", data.get("result_reason"));
        assertEquals("callback_without_user_task", data.get("result_fidelity"));
    }

    @Test
    void preexistingIdleRootUnknownUsesCallbackWithoutMatchingEventFidelity() {
        FabricChatClefCommandExecution execution = execution();
        IdleTask idle = new IdleTask();
        execution.markDispatchReturned(FabricChatClefTaskOwnershipEvidence.empty(), FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT);
        execution.openExecutorExecuteInvocation();
        execution.markFinishCallbackReceived(idle);
        execution.closeExecutorExecuteInvocation();

        Map<?, ?> data = data(execution.unknownFromPreexistingUnchangedIdleRoot().toMap());

        assertEquals("finish_callback_without_new_command_owned_root", data.get("result_reason"));
        assertEquals("callback_without_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void finishCallbackWithoutVerifiedSuccessUsesCallbackWithoutMatchingEventFidelity() {
        Map<?, ?> data = data(execution().unknownAfterFinish(null).toMap());

        assertEquals("finish_callback_without_verified_success", data.get("result_reason"));
        assertEquals("callback_without_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void matchingTaskFinishedUsesCallbackPlusMatchingEventFidelity() {
        FabricChatClefCommandTerminationObservation observation = observation(new IdleTask());

        Map<?, ?> data = data(execution().completedFromTaskFinished(observation).toMap());

        assertEquals("matching_task_finished", data.get("result_reason"));
        assertEquals("callback_plus_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void matchingTaskStoppedUsesCallbackPlusMatchingEventFidelity() {
        FabricChatClefCommandTerminationObservation observation = observation(new IdleTask());

        Map<?, ?> data = data(execution().failedFromStoppedTask(observation).toMap());

        assertEquals("matching_task_stopped", data.get("result_reason"));
        assertEquals("callback_plus_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void unclassifiedTaskObservationUsesCallbackPlusMatchingEventFidelity() {
        FabricChatClefCommandTerminationObservation observation = observation(new IdleTask());

        Map<?, ?> data = data(execution().unknownFromTaskObservation(observation).toMap());

        assertEquals("task_observation_unclassified", data.get("result_reason"));
        assertEquals("callback_plus_matching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void taskIdentityMismatchUsesCallbackPlusNonmatchingEventFidelity() {
        FabricChatClefCommandTerminationObservation observation = observation(new IdleTask());

        Map<?, ?> data = data(execution().unknownFromTaskIdentityMismatch(observation).toMap());

        assertEquals("task_identity_mismatch", data.get("result_reason"));
        assertEquals("callback_plus_nonmatching_user_task_event", data.get("result_fidelity"));
    }

    @Test
    void commandExceptionUsesCommandExceptionObservedFidelity() {
        Map<?, ?> data = data(execution().failedFromCommandException(
                new IllegalStateException("command failed"),
                FabricChatClefTaskSnapshot.capture(null)
        ).toMap());

        assertEquals("command_exception", data.get("result_reason"));
        assertEquals("command_exception_observed", data.get("result_fidelity"));
    }

    @Test
    void dispatchExceptionUsesDispatchExceptionObservedFidelity() {
        Map<?, ?> data = data(execution().failedFromDispatchException(
                new IllegalStateException("dispatch failed"),
                FabricChatClefTaskSnapshot.capture(null)
        ).toMap());

        assertEquals("dispatch_exception", data.get("result_reason"));
        assertEquals("dispatch_exception_observed", data.get("result_fidelity"));
    }

    @Test
    void deadlineExceededUsesDeadlineWithoutVerifiedTerminalFidelity() {
        Map<?, ?> data = data(execution().deadlineExceededResult("deadline").toMap());

        assertEquals("deadline_exceeded", data.get("result_reason"));
        assertEquals("deadline_without_verified_terminal", data.get("result_fidelity"));
    }

    @Test
    void unmappedDuplicateDiagnosticReasonUsesUnknownFidelity() {
        Map<?, ?> data = execution().duplicateTerminalPayload("duplicate_terminal_result").toMap();

        assertEquals("duplicate_terminal_result", data.get("result_reason"));
        assertEquals("unknown", data.get("result_fidelity"));
    }

    private static Map<?, ?> data(Map<String, Object> result) {
        return (Map<?, ?>) result.get("data");
    }

    private static FabricChatClefCommandTerminationObservation observation(IdleTask task) {
        return FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                new TaskFinishedEvent(0.25, task)
        );
    }

    private static FabricChatClefCommandExecution execution() {
        return new FabricChatClefCommandExecution(
                context(),
                "@deposit diamond 2",
                FabricChatClefTaskOwnershipEvidence.empty()
        );
    }

    private static FabricChatClefCommandContext context() {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = "req-fidelity";
        request.command = "deposit diamond 2";
        request.source = "test";
        return new FabricChatClefCommandContext(request, "corr-fidelity", "session-fidelity", 1L);
    }
}
