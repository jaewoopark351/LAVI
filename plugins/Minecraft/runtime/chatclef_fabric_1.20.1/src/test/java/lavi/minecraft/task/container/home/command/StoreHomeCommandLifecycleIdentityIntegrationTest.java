package lavi.minecraft.task.container.home.command;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.storehome.FabricChatClefStoreHomeResultProjector;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260829_kpopmodder: Prove one submitted StoreHomeTask identity reaches typed terminal projection unchanged.
class StoreHomeCommandLifecycleIdentityIntegrationTest {
    @Test
    void submittedRootRemainsIdenticalThroughObservationAndTypedProjection()
            throws Exception {
        StoreHomeTask suppliedRoot = new StoreHomeTaskFactory(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        ).create();
        AtomicInteger supplierCalls = new AtomicInteger();
        RecordingAltoClef mod = new RecordingAltoClef();
        StoreHomeCommand command = new StoreHomeCommand(
                () -> {
                    supplierCalls.incrementAndGet();
                    return suppliedRoot;
                },
                ignored -> true
        );

        command.run(mod, StoreHomeCommand.COMMAND_NAME, () -> { });

        assertEquals(1, supplierCalls.get());
        assertSame(suppliedRoot, mod.submittedRootTask);
        StoreHomeTask submittedRootTask = (StoreHomeTask) mod.submittedRootTask;
        invokeOnStop(submittedRootTask, new DummyTask());
        StoreHomeOutcome expectedOutcome = submittedRootTask.outcome()
                .orElseThrow();

        FabricChatClefCommandTerminationObservation terminationObservation =
                FabricChatClefCommandTerminationObservation
                        .fromTaskFinishedEvent(
                                new TaskFinishedEvent(
                                        1.0,
                                        submittedRootTask
                                )
                        );

        assertSame(submittedRootTask, terminationObservation.task());

        FabricChatClefCommandResultDataPayload basePayload =
                FabricChatClefCommandResultDataPayload.fromMap(
                        Map.of("result_reason", "identity_integration_test")
                );
        Map<String, Object> data = new FabricChatClefStoreHomeResultProjector()
                .fromMatchingTask(
                        basePayload,
                        "@store_home",
                        terminationObservation
                )
                .toMap();

        assertEquals("store_home", data.get("operation"));
        assertEquals(expectedOutcome.result().name(),
                data.get("store_home_result"));
        assertEquals(expectedOutcome.storedItems(), data.get("stored_items"));
        assertEquals(expectedOutcome.remainingStacks(),
                data.get("remaining_stacks"));
        assertEquals(expectedOutcome.reason(), data.get("reason"));
        assertEquals(expectedOutcome.goalSatisfied(),
                data.get("goal_satisfied"));
        assertEquals("identity_integration_test", data.get("result_reason"));
    }

    private static void invokeOnStop(
            StoreHomeTask task,
            Task interruptTask) {
        try {
            Method method = StoreHomeTask.class.getDeclaredMethod(
                    "onStop", Task.class
            );
            method.setAccessible(true);
            method.invoke(task, interruptTask);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError(
                    "Failed to invoke StoreHomeTask.onStop",
                    exception
            );
        } catch (InvocationTargetException exception) {
            throw new AssertionError(
                    "StoreHomeTask.onStop failed",
                    exception.getCause()
            );
        }
    }

    private static final class RecordingAltoClef extends AltoClef {
        private Task submittedRootTask;

        @Override
        public void runUserTask(Task task, Runnable onFinish) {
            submittedRootTask = task;
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
            return "store-home-command-lifecycle-identity-test";
        }
    }
}
