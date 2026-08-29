package lavi.minecraft.task.container.home.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260827_kpopmodder: Added focused tests for safety suspension versus user-task replacement.
class StoreHomeTaskInterruptionTest {
    @Test
    void nullSafetyInterruptionSuspendsWithoutCreatingATerminalResult() {
        StoreHomeTask task = task();

        invokeOnStop(task, null);

        assertEquals(StoreHomeResult.PENDING, task.result());
        assertEquals(StoreHomePhase.SUSPENDED, task.phase());
        assertTrue(task.outcome().isEmpty());
    }

    @Test
    void incomingUserTaskReplacementIsTerminalInterrupted() {
        StoreHomeTask task = task();

        invokeOnStop(task, new DummyTask());

        assertEquals(StoreHomeResult.INTERRUPTED, task.result());
        assertEquals(StoreHomePhase.TERMINAL, task.phase());
        assertTrue(task.outcome().isPresent());
        assertEquals(StoreHomeResult.INTERRUPTED, task.outcome().orElseThrow().result());
        assertEquals("replaced_by_new_user_task", task.outcome().orElseThrow().reason());
        assertFalse(task.outcome().orElseThrow().goalSatisfied());
    }

    @Test
    void safetyPauseAndResumePreserveCandidateAndOperationClocks() {
        StoreHomeTask task = task();
        StoreHomeExecutionState executionState = executionState(task);
        executionState.lifecycle().markInitialized();
        StoreHomeTimeoutLifecycle lifecycle = timeoutLifecycle(task);
        lifecycle.startCandidate(100.0, 64.0, 100.0);
        lifecycle.onActiveRootTick();
        lifecycle.observeCandidate(true, 0.0, 64.0, 0.0, false);
        StoreHomeTimeoutObservation beforePause = lifecycle.observation();

        invokeOnStop(task, null);
        invokeOnStart(task);

        assertEquals(StoreHomePhase.REVALIDATE_AFTER_RESUME, task.phase());
        assertEquals(beforePause, lifecycle.observation());
    }

    private static StoreHomeTask task() {
        return new StoreHomeTaskFactory(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        ).create();
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

    private static void invokeOnStart(StoreHomeTask task) {
        try {
            Method method = StoreHomeTask.class.getDeclaredMethod("onStart");
            method.setAccessible(true);
            method.invoke(task);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError("Failed to invoke StoreHomeTask.onStart", exception);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("StoreHomeTask.onStart failed", exception.getCause());
        }
    }

    private static StoreHomeTimeoutLifecycle timeoutLifecycle(StoreHomeTask task) {
        return field(task, "timeoutLifecycle", StoreHomeTimeoutLifecycle.class);
    }

    private static StoreHomeExecutionState executionState(StoreHomeTask task) {
        return field(task, "state", StoreHomeExecutionState.class);
    }

    private static <T> T field(
            StoreHomeTask task,
            String fieldName,
            Class<T> fieldType) {
        try {
            Field field = StoreHomeTask.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(task));
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new AssertionError(
                    "Failed to read StoreHomeTask." + fieldName, exception
            );
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
            return "store-home-interruption-test";
        }
    }
}
