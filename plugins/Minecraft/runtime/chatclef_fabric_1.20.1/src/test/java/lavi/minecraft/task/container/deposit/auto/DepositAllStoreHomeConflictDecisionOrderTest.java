package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Fix exact StoreHome-root suppression ahead of generic selected-chain admission.
class DepositAllStoreHomeConflictDecisionOrderTest {
    private static final DepositAllInventoryPressureSnapshot HIGH_WATER =
            new DepositAllInventoryPressureSnapshot(33, 36);

    @Test
    void assignedStoreHomeRootSuppressesAutomaticSubmissionBeforeItsFirstTick() {
        Fixture fixture = fixture();

        assertFalse(fixture.storeHome.isActive());
        assertTrue(fixture.auto.suppressExistingStoreHome(
                HIGH_WATER,
                fixture.user,
                fixture.storeHome
        ));

        assertSuppressed(fixture);
    }

    @Test
    void safetySelectedStoreHomeRootStillWinsBeforeGenericSelectedChainGate() {
        Fixture fixture = fixture();
        SafetyChain safety = new SafetyChain(fixture.runner);
        set(fixture.runner, "cachedCurrentTaskChain", safety);

        assertTrue(fixture.auto.suppressExistingStoreHome(
                HIGH_WATER,
                fixture.user,
                fixture.storeHome
        ));

        assertSame(safety, fixture.runner.getCurrentTaskChain());
        assertSuppressed(fixture);
    }

    @Test
    void stoppedButUnclearedStoreHomeRootRemainsTheExactSuppressionAuthority() {
        Fixture fixture = fixture();
        set(fixture.storeHome, "stopped", true);

        assertTrue(fixture.storeHome.stopped());
        assertTrue(fixture.auto.suppressExistingStoreHome(
                HIGH_WATER,
                fixture.user,
                fixture.storeHome
        ));

        assertSuppressed(fixture);
    }

    @Test
    void staleCapturedStoreHomeIdentityCannotSuppressAReplacementRoot() {
        Fixture fixture = fixture();
        Task replacement = new UnrelatedTask();
        fixture.user.setTask(replacement);

        assertFalse(fixture.auto.suppressExistingStoreHome(
                HIGH_WATER,
                fixture.user,
                fixture.storeHome
        ));
        assertEquals(DepositAllInventoryPressureState.ARMED, stateMachine(fixture.auto).state());
        assertSame(replacement, fixture.user.getCurrentTask());
    }

    private static Fixture fixture() {
        TestAltoClef mod = new TestAltoClef();
        TaskRunner runner = new TaskRunner(mod);
        UserTaskChain user = new UserTaskChain(runner);
        mod.runner = runner;
        mod.user = user;
        StoreHomeTask storeHome = allocate(StoreHomeTask.class);
        user.setTask(storeHome);
        DepositAllInventoryPressureChain auto =
                new DepositAllInventoryPressureChain(runner);
        return new Fixture(runner, user, storeHome, auto);
    }

    private static void assertSuppressed(Fixture fixture) {
        assertEquals(
                DepositAllInventoryPressureState.WAIT_FOR_REARM,
                stateMachine(fixture.auto).state()
        );
        assertNull(fixture.auto.getCurrentTask());
        assertSame(fixture.storeHome, fixture.user.getCurrentTask());
        assertNull(read(stateMachine(fixture.auto), "noSafeFingerprint"));
    }

    private static DepositAllInventoryPressureStateMachine stateMachine(
            DepositAllInventoryPressureChain chain) {
        return (DepositAllInventoryPressureStateMachine) read(chain, "stateMachine");
    }

    private static Object read(Object owner, String fieldName) {
        try {
            Field field = field(owner.getClass(), fieldName);
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to read " + fieldName, exception);
        }
    }

    private static void set(Object owner, String fieldName, Object value) {
        try {
            Field field = field(owner.getClass(), fieldName);
            field.set(owner, value);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to write " + fieldName, exception);
        }
    }

    private static Field field(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new AssertionError("Missing field " + type.getName() + "." + fieldName);
    }

    private static <T> T allocate(Class<T> type) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            return type.cast(unsafe.allocateInstance(type));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to allocate " + type.getName(), exception);
        }
    }

    private record Fixture(
            TaskRunner runner,
            UserTaskChain user,
            StoreHomeTask storeHome,
            DepositAllInventoryPressureChain auto) {
    }

    private static final class TestAltoClef extends AltoClef {
        private TaskRunner runner;
        private UserTaskChain user;

        @Override
        public TaskRunner getTaskRunner() {
            return runner;
        }

        @Override
        public UserTaskChain getUserTaskChain() {
            return user;
        }
    }

    private static final class SafetyChain extends TaskChain {
        private SafetyChain(TaskRunner runner) {
            super(runner);
        }

        @Override
        protected void onStop() {
        }

        @Override
        public void onInterrupt(TaskChain other) {
        }

        @Override
        protected void onTick() {
        }

        @Override
        public float getPriority() {
            return 100.0f;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public String getName() {
            return "store-home conflict safety fixture";
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
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "replacement user root";
        }
    }
}
