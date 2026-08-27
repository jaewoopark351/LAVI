package lavi.minecraft.task.container.home.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260827_kpopmodder: Added focused tests for safety suspension versus user-task replacement.
class StoreHomeTaskInterruptionTest {
    @Test
    void nullSafetyInterruptionSuspendsWithoutCreatingATerminalResult() {
        StoreHomeTask task = task();

        invokeOnStop(task, null);

        assertEquals(StoreHomeResult.PENDING, task.result());
        assertEquals(StoreHomePhase.SUSPENDED, task.phase());
    }

    @Test
    void incomingUserTaskReplacementIsTerminalInterrupted() {
        StoreHomeTask task = task();

        invokeOnStop(task, new DummyTask());

        assertEquals(StoreHomeResult.INTERRUPTED, task.result());
        assertEquals(StoreHomePhase.TERMINAL, task.phase());
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
