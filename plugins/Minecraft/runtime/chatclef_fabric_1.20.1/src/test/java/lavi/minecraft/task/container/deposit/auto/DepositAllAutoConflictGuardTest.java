package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasks.container.StoreInAnyContainerTask;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllAutoConflictGuardTest {
    private final DepositAllAutoConflictGuard guard = new DepositAllAutoConflictGuard();

    @Test
    void detectsEveryManualDepositRouteWithoutMatchingUnrelatedTasks() {
        assertFalse(hasConflict(null));
        assertFalse(hasConflict(new UnrelatedTask()));
        assertTrue(hasConflict(allocate(DepositAllTask.class)));
        assertTrue(hasConflict(allocate(StoreInAnyContainerTask.class)));
        assertTrue(guard.isDepositRouteClass(StoreInContainerTask.class));
    }

    @Test
    void detectsADepositRouteNestedUnderTheCurrentUserTask() {
        UnrelatedTask parent = new UnrelatedTask();
        setChild(parent, allocate(DepositAllTask.class));

        assertTrue(hasConflict(parent));
    }

    @Test
    void recognizesOnlyAnExactStoreHomeRootWithoutTreatingItsParentAsStoreHome() {
        StoreHomeTask storeHome = allocate(StoreHomeTask.class);
        UnrelatedTask parent = new UnrelatedTask();
        setChild(parent, storeHome);

        assertTrue(guard.isStoreHomeRoot(storeHome));
        assertFalse(guard.isStoreHomeRoot(parent));
        assertFalse(guard.hasExistingDepositTask(storeHome));
        assertFalse(guard.hasExistingDepositTask(parent));
    }

    private boolean hasConflict(Task task) {
        TestAltoClef mod = new TestAltoClef();
        UserTaskChain userTaskChain = new UserTaskChain(new TaskRunner(mod));
        mod.userTaskChain = userTaskChain;
        if (task != null) {
            userTaskChain.setTask(task);
        }
        return guard.hasExistingDepositTask(mod);
    }

    private static void setChild(Task parent, Task child) {
        try {
            Field field = Task.class.getDeclaredField("sub");
            field.setAccessible(true);
            field.set(parent, child);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create nested deposit route fixture", exception);
        }
    }

    private static <T> T allocate(Class<T> type) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            return type.cast(unsafe.allocateInstance(type));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to allocate headless deposit route fixture", exception);
        }
    }

    private static final class TestAltoClef extends AltoClef {
        private UserTaskChain userTaskChain;

        @Override
        public UserTaskChain getUserTaskChain() {
            return userTaskChain;
        }
    }

    private static final class UnrelatedTask extends Task {
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
            return other instanceof UnrelatedTask;
        }

        @Override
        protected String toDebugString() {
            return "unrelated automatic deposit_all conflict test task";
        }
    }
}
