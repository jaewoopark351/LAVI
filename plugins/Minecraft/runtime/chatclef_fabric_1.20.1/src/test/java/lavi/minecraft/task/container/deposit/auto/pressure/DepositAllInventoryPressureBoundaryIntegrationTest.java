package lavi.minecraft.task.container.deposit.auto.pressure;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.player2api.AICommandBridge;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureChain;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureState;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureStateMachine;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260831_kpopmodder: Prove the 32-to-33 main-slot boundary through the production pressure callback.
class DepositAllInventoryPressureBoundaryIntegrationTest {

    @AfterEach
    void resetDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    //20260913_kpopmodder: Diagnostics must not perform a skipped inventory read or consume its original failure.
    @Test
    void diagnosticsPreservePressureReadCountsAndExceptions() {
        for (boolean enabled : new boolean[]{false, true}) {
            ChatClefDiagnostics.setBoundaryEnabled(enabled);
            try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.inGame()) {
                TestAltoClef mod = new TestAltoClef();
                TaskRunner runner = new TaskRunner(mod);
                UserTaskChain user = new UserTaskChain(runner);
                AICommandBridge bridge = TestObjects.allocate(AICommandBridge.class);
                mod.runner = runner;
                mod.user = user;
                mod.bridge = bridge;
                MutablePressureSource source = new MutablePressureSource(32);
                DepositAllInventoryPressureChain pressure = pressureChain(runner, source);

                bridge.setEnabled(false);
                pressure.onEndClientTick();
                assertEquals(0, source.readCalls);
                bridge.setEnabled(true);
                pressure.onEndClientTick();
                assertEquals(1, source.readCalls);
                assertEquals(DepositAllInventoryPressureState.ARMED, stateMachine(pressure).state());

                source.failRead = true;
                assertThrows(IllegalStateException.class, pressure::onEndClientTick);
                assertEquals(2, source.readCalls);
                assertEquals(DepositAllInventoryPressureState.ARMED, stateMachine(pressure).state());
            }
        }
    }

    @Test
    void thirtyTwoSlotsStayArmedAndThirtyThreeSuppressAgainstStoreHome() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.inGame()) {
            TestAltoClef mod = new TestAltoClef();
            TaskRunner runner = new TaskRunner(mod);
            UserTaskChain user = new UserTaskChain(runner);
            AICommandBridge bridge = TestObjects.allocate(AICommandBridge.class);
            bridge.setEnabled(true);
            mod.runner = runner;
            mod.user = user;
            mod.bridge = bridge;

            StoreHomeTask storeHome = TestObjects.allocate(StoreHomeTask.class);
            user.setTask(storeHome);
            MutablePressureSource source = new MutablePressureSource(32);
            DepositAllInventoryPressureChain pressure = pressureChain(runner, source);

            pressure.onEndClientTick();

            assertEquals(DepositAllInventoryPressureState.ARMED, stateMachine(pressure).state());
            assertNull(pressure.getCurrentTask());
            assertSame(storeHome, user.getCurrentTask());

            source.setOccupiedSlots(33);
            pressure.onEndClientTick();

            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(pressure).state()
            );
            assertNull(pressure.getCurrentTask());
            assertSame(storeHome, user.getCurrentTask());
        }
    }

    private static DepositAllInventoryPressureChain pressureChain(
            TaskRunner runner,
            AutoDepositInventoryPressureSource source) {
        DepositAllInventoryPressureChain chain =
                new DepositAllInventoryPressureChain(runner);
        TestObjects.setField(
                chain,
                DepositAllInventoryPressureChain.class,
                "pressureSource",
                source
        );
        return chain;
    }

    private static DepositAllInventoryPressureStateMachine stateMachine(
            DepositAllInventoryPressureChain chain) {
        try {
            Field field = DepositAllInventoryPressureChain.class.getDeclaredField("stateMachine");
            field.setAccessible(true);
            return (DepositAllInventoryPressureStateMachine) field.get(chain);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect automatic-deposit pressure state", exception);
        }
    }

    private static final class TestAltoClef extends AltoClef {
        private TaskRunner runner;
        private UserTaskChain user;
        private AICommandBridge bridge;

        @Override
        public TaskRunner getTaskRunner() {
            return runner;
        }

        @Override
        public UserTaskChain getUserTaskChain() {
            return user;
        }

        @Override
        public AICommandBridge getAiBridge() {
            return bridge;
        }
    }

    private static final class MutablePressureSource
            implements AutoDepositInventoryPressureSource {
        private DepositAllInventoryPressureSnapshot snapshot;
        private int readCalls;
        private boolean failRead;

        private MutablePressureSource(int occupiedSlots) {
            setOccupiedSlots(occupiedSlots);
        }

        private void setOccupiedSlots(int occupiedSlots) {
            snapshot = new DepositAllInventoryPressureSnapshot(occupiedSlots, 36);
        }

        @Override
        public Optional<DepositAllInventoryPressureSnapshot> read(AltoClef mod) {
            readCalls++;
            if (failRead) throw new IllegalStateException("original pressure read failure");
            return Optional.of(snapshot);
        }
    }
}
