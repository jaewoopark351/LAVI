package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
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

    private static Map<?, ?> data(Map<String, Object> result) {
        return (Map<?, ?>) result.get("data");
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
