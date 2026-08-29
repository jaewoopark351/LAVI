package lavi.minecraft.fabric.chatclef.bridge.command.result.storehome;

import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

//20260827_kpopmodder: Verify typed STORE_HOME projection without parsing logs or debug strings.
class FabricChatClefStoreHomeResultProjectorTest {
    @Test
    void commandGateRejectionIsOwnedByTheCurrentExecutionResult() {
        FabricChatClefCommandRequest request = request("store-home-gate");
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                new FabricChatClefCommandContext(request, "correlation", "session", 1L),
                "@store_home",
                FabricChatClefTaskOwnershipEvidence.empty()
        );

        Map<String, Object> data = resultData(execution.completedWithoutUserTask().toMap());

        assertEquals("store_home", data.get("operation"));
        assertEquals("CURSOR_NOT_EMPTY", data.get("store_home_result"));
        assertEquals(0, data.get("stored_items"));
        assertEquals(0, data.get("remaining_stacks"));
        assertEquals("cursor_not_empty_at_command_gate", data.get("reason"));
        assertEquals(Boolean.FALSE, data.get("goal_satisfied"));
    }

    @Test
    void matchingStoreHomeTaskProjectsItsTypedTerminalOutcome() {
        StoreHomeTask task = new StoreHomeTaskFactory(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        ).create();
        invokeOnStop(task, new DummyTask());
        FabricChatClefCommandTerminationObservation observation =
                FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(
                        new TaskFinishedEvent(1.0, task)
                );
        FabricChatClefCommandResultDataPayload base =
                FabricChatClefCommandResultDataPayload.fromMap(Map.of("result_reason", "test"));

        Map<String, Object> data = new FabricChatClefStoreHomeResultProjector()
                .fromMatchingTask(base, "@store_home", observation)
                .toMap();

        assertEquals("INTERRUPTED", data.get("store_home_result"));
        assertEquals("replaced_by_new_user_task", data.get("reason"));
        assertEquals(Boolean.FALSE, data.get("goal_satisfied"));
        assertEquals("test", data.get("result_reason"));
    }

    @Test
    void unrelatedCommandsNeverReceiveStoreHomeFields() {
        FabricChatClefCommandResultDataPayload base =
                FabricChatClefCommandResultDataPayload.fromMap(Map.of("result_reason", "test"));

        Map<String, Object> data = new FabricChatClefStoreHomeResultProjector()
                .fromCommandWithoutUserTask(base, "@get diamond 1")
                .toMap();

        assertFalse(data.containsKey("operation"));
        assertFalse(data.containsKey("store_home_result"));
    }

    @Test
    void everyTerminalEnumKeepsItsExactWireIdentityAndGoalMeaning() {
        for (StoreHomeResult result : StoreHomeResult.values()) {
            if (result == StoreHomeResult.PENDING) {
                continue;
            }
            StoreHomeOutcome outcome = StoreHomeOutcome.terminal(
                    result,
                    result.name().startsWith("PARTIAL_") ? 1 : 0,
                    result.name().startsWith("PARTIAL_") ? 1 : 0,
                    "test_reason"
            );

            Map<String, Object> data = new FabricChatClefStoreHomeResultDataPayload(
                    FabricChatClefCommandResultDataPayload.empty(),
                    outcome
            ).toMap();

            assertEquals(result.name(), data.get("store_home_result"));
            assertEquals(
                    Boolean.valueOf(result == StoreHomeResult.COMPLETED),
                    data.get("goal_satisfied")
            );
        }
    }

    @Test
    void reportingOnlyZeroRemainingPreservesPartialWireMeaning() {
        StoreHomeOutcome outcome = StoreHomeOutcome.terminal(
                StoreHomeResult.PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE,
                52,
                0,
                "trusted_candidates_exhausted"
        );

        Map<String, Object> data = new FabricChatClefStoreHomeResultDataPayload(
                FabricChatClefCommandResultDataPayload.empty(),
                outcome
        ).toMap();

        assertEquals(6, data.size());
        assertEquals("store_home", data.get("operation"));
        assertEquals(
                "PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE",
                data.get("store_home_result")
        );
        assertEquals(52, data.get("stored_items"));
        assertEquals(0, data.get("remaining_stacks"));
        assertEquals(Boolean.FALSE, data.get("goal_satisfied"));
    }

    @Test
    void operationTimeoutStableReasonReachesTheWireUnchanged() {
        StoreHomeOutcome outcome = StoreHomeOutcome.terminal(
                StoreHomeResult.NO_USABLE_TRUSTED_DESTINATION,
                0,
                1,
                "operation_no_progress"
        );

        Map<String, Object> data = new FabricChatClefStoreHomeResultDataPayload(
                FabricChatClefCommandResultDataPayload.empty(),
                outcome
        ).toMap();

        assertEquals("operation_no_progress", data.get("reason"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resultData(Map<String, Object> result) {
        return (Map<String, Object>) result.get("data");
    }

    private static FabricChatClefCommandRequest request(String requestId) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = "store_home";
        request.source = "lavi_chat_mic_router";
        return request;
    }

    private static void invokeOnStop(StoreHomeTask task, Task interruptTask) {
        try {
            Method method = StoreHomeTask.class.getDeclaredMethod("onStop", Task.class);
            method.setAccessible(true);
            method.invoke(task, interruptTask);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError("Failed to invoke StoreHomeTask.onStop", exception);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("StoreHomeTask.onStop failed", exception.getCause());
        }
    }

    private static final class DummyTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "store-home-result-projector-test";
        }
    }
}
