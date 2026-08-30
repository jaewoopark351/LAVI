package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.player2api.AICommandBridge;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.composition.DepositAllInventoryPressureChainPreparation;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenancePhase;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.testsupport.auto.AutoDepositPlanFixtureFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove real TaskRunner reconciliation from an automatic root to the exact StoreHome root.
class DepositAllStoreHomeTaskRunnerHandoffTest {
    @Test
    void automaticContextCancelsBeforeChildWorkThenReconcilesToStoreHome() {
        try (RunnerFixture fixture = new RunnerFixture()) {
            Task originalUserRoot = new UnrelatedTask("original user root");
            fixture.user.setTask(originalUserRoot);
            AutoDepositMaintenanceTask maintenance = maintenanceFor(originalUserRoot, 41L);
            Task eagerPrimary = maintenance.primaryDepositTask();
            assertFalse(eagerPrimary.isActive());
            assertFalse(eagerPrimary.stopped());
            assertEquals(Boolean.TRUE, read(eagerPrimary, Task.class, "first"));
            moveToRunning(fixture.automatic);
            fixture.automatic.setTask(maintenance);

            StoreHomeTask storeHome = TestObjects.allocate(StoreHomeTask.class);
            fixture.user.setTask(storeHome);

            fixture.runner.tick();

            assertSame(fixture.automatic, fixture.runner.getCurrentTaskChain());
            assertEquals(AutoDepositMaintenancePhase.CANCELLED, maintenance.phase());
            assertEquals(AutoDepositMaintenanceOutcome.CANCELLED, maintenance.outcome());
            assertNull(read(maintenance, Task.class, "sub"));
            assertSame(eagerPrimary, maintenance.depositTask());
            assertFalse(eagerPrimary.isActive());
            assertFalse(eagerPrimary.stopped());
            assertEquals(Boolean.TRUE, read(eagerPrimary, Task.class, "first"));
            assertNull(read(maintenance, AutoDepositMaintenanceTask.class, "trustedTask"));
            assertTrue(maintenance.manifest().positions().isEmpty());
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertEquals(0, fixture.user.tickCalls);

            fixture.runner.tick();

            assertSame(fixture.automatic, fixture.runner.getCurrentTaskChain());
            assertNull(fixture.automatic.getCurrentTask());
            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(fixture.automatic).state()
            );

            fixture.runner.tick();

            assertSame(fixture.user, fixture.runner.getCurrentTaskChain());
            assertEquals(1, fixture.user.tickCalls);
            assertSame(storeHome, fixture.user.getCurrentTask());
        }
    }

    @Test
    void automaticRootSubmittedBeforeFirstRunnerTickWaitsBehindSafetyThenHandsOff() {
        try (RunnerFixture fixture = new RunnerFixture()) {
            Task originalUserRoot = new UnrelatedTask("pre-safety user root");
            fixture.user.setTask(originalUserRoot);
            AutoDepositMaintenanceTask maintenance = maintenanceFor(originalUserRoot, 42L);
            Task eagerPrimary = maintenance.primaryDepositTask();
            ToggleSafetyChain safety = new ToggleSafetyChain(fixture.runner);
            moveToRunning(fixture.automatic);
            fixture.automatic.setTask(maintenance);
            StoreHomeTask storeHome = TestObjects.allocate(StoreHomeTask.class);
            fixture.user.setTask(storeHome);
            safety.active = true;

            fixture.runner.tick();

            assertSame(safety, fixture.runner.getCurrentTaskChain());
            assertSame(maintenance, fixture.automatic.getCurrentTask());
            assertFalse(maintenance.isActive());
            assertFalse(eagerPrimary.isActive());
            assertFalse(eagerPrimary.stopped());
            assertEquals(Boolean.TRUE, read(eagerPrimary, Task.class, "first"));
            assertEquals(AutoDepositMaintenanceOutcome.PENDING, maintenance.outcome());
            assertEquals(0, fixture.user.tickCalls);

            safety.active = false;
            fixture.runner.tick();

            assertSame(fixture.automatic, fixture.runner.getCurrentTaskChain());
            assertEquals(AutoDepositMaintenancePhase.CANCELLED, maintenance.phase());
            assertEquals(AutoDepositMaintenanceOutcome.CANCELLED, maintenance.outcome());
            assertNull(read(maintenance, Task.class, "sub"));
            assertFalse(eagerPrimary.isActive());
            assertFalse(eagerPrimary.stopped());
            assertEquals(Boolean.TRUE, read(eagerPrimary, Task.class, "first"));

            fixture.runner.tick();
            assertNull(fixture.automatic.getCurrentTask());
            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(fixture.automatic).state()
            );

            fixture.runner.tick();
            assertSame(fixture.user, fixture.runner.getCurrentTaskChain());
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertEquals(1, fixture.user.tickCalls);
            assertEquals(0, fixture.runner.disableCalls);
        }
    }

    @Test
    void assignedStoreHomeWinsTheFirstRealRunnerSelectionWithoutAutomaticSubmission() {
        try (RunnerFixture fixture = new RunnerFixture()) {
            StoreHomeTask storeHome = TestObjects.allocate(StoreHomeTask.class);
            fixture.user.setTask(storeHome);

            fixture.automatic.onEndClientTick();

            fixture.runner.tick();

            assertSame(fixture.user, fixture.runner.getCurrentTaskChain());
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertEquals(1, fixture.user.tickCalls);
            assertNull(fixture.automatic.getCurrentTask());
            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(fixture.automatic).state()
            );
            assertEquals(0, fixture.runner.disableCalls);
        }
    }

    @Test
    void safetySelectedStoreHomeStillSuppressesThroughTheProductionPressureCallback() {
        try (RunnerFixture fixture = new RunnerFixture()) {
            StoreHomeTask storeHome = TestObjects.allocate(StoreHomeTask.class);
            fixture.user.setTask(storeHome);
            ToggleSafetyChain safety = new ToggleSafetyChain(fixture.runner);
            safety.active = true;

            fixture.runner.tick();
            assertSame(safety, fixture.runner.getCurrentTaskChain());

            fixture.automatic.onEndClientTick();

            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(fixture.automatic).state()
            );
            assertNull(fixture.automatic.getCurrentTask());
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertFalse(storeHome.isActive());
            assertEquals(0, fixture.user.tickCalls);

            safety.active = false;
            fixture.runner.tick();

            assertSame(fixture.user, fixture.runner.getCurrentTaskChain());
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertEquals(1, fixture.user.tickCalls);
            assertEquals(0, fixture.runner.disableCalls);
        }
    }

    @Test
    void safetyPreemptionStopsOnlyTheAutomaticTreeOnceThenHandsOffToStoreHome() {
        try (RunnerFixture fixture = new RunnerFixture()) {
            TrackingTask child = new TrackingTask(null);
            TrackingTask root = new TrackingTask(child);
            ToggleSafetyChain safety = new ToggleSafetyChain(fixture.runner);
            moveToRunning(fixture.automatic);
            fixture.automatic.setTask(root);

            fixture.runner.tick();

            assertSame(fixture.automatic, fixture.runner.getCurrentTaskChain());
            assertTrue(root.isActive());
            assertTrue(child.isActive());
            assertEquals(1, root.tickCalls);
            assertEquals(1, child.tickCalls);

            StoreHomeTask storeHome = TestObjects.allocate(StoreHomeTask.class);
            fixture.user.setTask(storeHome);
            safety.active = true;

            fixture.runner.tick();

            assertSame(safety, fixture.runner.getCurrentTaskChain());
            assertEquals(1, root.stopCalls);
            assertEquals(1, child.stopCalls);
            assertEquals(1, root.tickCalls);
            assertEquals(1, child.tickCalls);
            assertNull(fixture.automatic.getCurrentTask());
            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(fixture.automatic).state()
            );
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertEquals(0, fixture.user.tickCalls);
            assertFalse(storeHome.isActive());

            fixture.runner.tick();

            assertEquals(1, root.stopCalls);
            assertEquals(1, child.stopCalls);
            assertEquals(1, root.tickCalls);
            assertEquals(1, child.tickCalls);

            safety.active = false;
            fixture.runner.tick();

            assertSame(fixture.user, fixture.runner.getCurrentTaskChain());
            assertSame(storeHome, fixture.user.getCurrentTask());
            assertEquals(1, fixture.user.tickCalls);
            assertEquals(0, fixture.runner.disableCalls);
        }
    }

    private static AutoDepositMaintenanceTask maintenanceFor(Task userRoot, long epoch) {
        Object operationWorld = new Object();
        WorkingSetSnapshot workingSet = new WorkingSetSnapshot(
                userRoot,
                List.of(userRoot),
                operationWorld,
                Dimension.OVERWORLD,
                epoch,
                Map.of(),
                Map.of(),
                Map.of()
        );
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(
                operationWorld,
                Dimension.OVERWORLD,
                "handoff-world",
                epoch,
                userRoot,
                workingSet,
                List.of("user-root")
        );
        AutoDepositPlan plan = AutoDepositPlanFixtureFactory.generalPlan(
                context,
                userRoot,
                33,
                5,
                1
        );
        AutoDepositMaintenanceTask maintenance = new AutoDepositMaintenanceTask(
                plan,
                AutoDepositTrustedDestinationRepository.inMemoryEmpty()
        );
        // The bounded test client deliberately avoids bootstrapping Minecraft's
        // global registries. Null keeps the world comparison stable so this test
        // isolates root replacement; production snapshots still require non-null.
        TestObjects.setField(context, AutoDepositContextSnapshot.class, "worldIdentity", null);
        return maintenance;
    }

    private static DepositAllInventoryPressureChain automaticChain(TaskRunner runner) {
        AutoDepositPolicyEngine engine = AutoDepositPolicyEngine.inMemoryDefault();
        DepositAllInventoryPressureChainPreparation base =
                DepositAllInventoryPressureChainPreparation.prepare(runner, engine);
        return DepositAllInventoryPressureChain.commit(
                new DepositAllInventoryPressureChainPreparation(
                        base.runner(),
                        base.mod(),
                        ignored -> java.util.Optional.of(
                                new DepositAllInventoryPressureSnapshot(33, 36)
                        ),
                        base.stateMachine(),
                        base.conflictGuard(),
                        base.workingSetResolver(),
                        base.policyEngine(),
                        base.trustedRepository(),
                        base.exactOpenContainerBinding(),
                        base.initialTrustedRevision()
                )
        );
    }

    private static void moveToRunning(DepositAllInventoryPressureChain chain) {
        DepositAllInventoryPressureStateMachine machine = stateMachine(chain);
        assertEquals(
                DepositAllInventoryPressureSignal.THRESHOLD_REACHED,
                machine.observe(new DepositAllInventoryPressureSnapshot(33, 36))
        );
        machine.markRunStarted();
    }

    private static DepositAllInventoryPressureStateMachine stateMachine(
            DepositAllInventoryPressureChain chain) {
        return (DepositAllInventoryPressureStateMachine) read(
                chain,
                DepositAllInventoryPressureChain.class,
                "stateMachine"
        );
    }

    private static Object read(Object target, Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to read " + owner.getName() + "." + name, exception);
        }
    }

    private static void set(Object target, String name, Object value) {
        Class<?> owner = target.getClass();
        while (owner != null) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                owner = owner.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new AssertionError("Failed to write " + name, exception);
            }
        }
        throw new AssertionError("Missing field " + name);
    }

    private static Object readStatic(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to read static " + owner.getName() + "." + name, exception);
        }
    }

    private static void setStatic(Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to write static " + owner.getName() + "." + name, exception);
        }
    }

    private static final class TestAltoClef extends AltoClef {
        private RecordingTaskRunner runner;
        private RecordingUserTaskChain user;
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

    private static final class RecordingTaskRunner extends TaskRunner {
        private int disableCalls;

        private RecordingTaskRunner(AltoClef mod) {
            super(mod);
        }

        @Override
        public void disable() {
            disableCalls++;
            super.disable();
        }
    }

    private static final class RecordingUserTaskChain extends UserTaskChain {
        private int tickCalls;

        private RecordingUserTaskChain(TaskRunner runner) {
            super(runner);
        }

        @Override
        public void tick() {
            tickCalls++;
        }
    }

    private static final class UnrelatedTask extends Task {
        private final String name;

        private UnrelatedTask(String name) {
            this.name = name;
        }

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
            return name;
        }
    }

    private static final class TrackingTask extends Task {
        private final Task child;
        private int stopCalls;
        private int tickCalls;

        private TrackingTask(Task child) {
            this.child = child;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            tickCalls++;
            return child;
        }

        @Override
        protected void onStop(Task interruptTask) {
            stopCalls++;
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "automatic runner handoff task";
        }
    }

    private static final class ToggleSafetyChain extends TaskChain {
        private boolean active;

        private ToggleSafetyChain(TaskRunner runner) {
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
            return active;
        }

        @Override
        public String getName() {
            return "automatic runner handoff safety";
        }
    }

    private static final class RunnerFixture implements AutoCloseable {
        private final Object previousAltoClef = readStatic(AltoClef.class, "instance");
        private final HeadlessMinecraftClientSession clientSession =
                HeadlessMinecraftClientSession.inGame();
        private final TestAltoClef mod = new TestAltoClef();
        private final RecordingTaskRunner runner = new RecordingTaskRunner(mod);
        private final RecordingUserTaskChain user = new RecordingUserTaskChain(runner);
        private final DepositAllInventoryPressureChain automatic;

        private RunnerFixture() {
            mod.runner = runner;
            mod.user = user;
            mod.bridge = TestObjects.allocate(AICommandBridge.class);
            mod.bridge.setEnabled(true);
            setStatic(AltoClef.class, "instance", mod);
            set(runner, "active", true);
            automatic = automaticChain(runner);
        }

        @Override
        public void close() {
            setStatic(AltoClef.class, "instance", previousAltoClef);
            clientSession.close();
        }
    }
}
