package lavi.minecraft.task.container.deposit.auto.admission;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.MobDefenseChain;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoConflictGuard;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Keep native safety and manual storage admission ahead of automatic planning.
class AutoDepositPlanAdmissionTest {
    @Test
    void manualStorageDefersBeforeSafetyOrPlanningAndRetainsItsRoot() {
        Mod mod = TestObjects.allocate(Mod.class);
        Runner runner = new Runner(mod);
        mod.user = TestObjects.allocate(User.class);
        mod.user.active = true;
        mod.user.root = TestObjects.allocate(StoreHomeTask.class);
        mod.failIfDefenseRead = true;
        AutoDepositPlanAdmission admission = admission(mod, runner);
        AutoDepositAdmissionResult result = admission.plan(null, new DepositAllInventoryPressureSnapshot(36, 36));
        assertEquals("manual_storage", result.reason());
        assertTrue(result.deferred());
        assertSame(mod.user.root, result.root());
        assertNull(result.workingSet());
        assertNull(result.planning());
    }

    @Test
    void safetyClaimDefersBeforeAnyWorkingSetOrPlanIsRequired() {
        Mod mod = TestObjects.allocate(Mod.class);
        Runner runner = new Runner(mod);
        mod.defense = TestObjects.allocate(Defense.class);
        AutoDepositAdmissionResult result = admission(mod, runner)
                .plan(null, new DepositAllInventoryPressureSnapshot(36, 36));
        assertEquals("survival_claim", result.reason());
        assertTrue(result.deferred());
        assertNull(result.planning());
    }

    @Test
    void selectedNativeChainDefersButAutomaticOwnerAndUserChainAreAllowed() {
        Mod mod = TestObjects.allocate(Mod.class);
        Runner runner = new Runner(mod);
        PassiveChain owner = new PassiveChain(runner);
        PassiveChain safety = new PassiveChain(runner);
        AutoDepositPlanAdmission admission = admission(mod, runner);
        runner.selected = safety;
        assertEquals("native_chain_selected", admission.controlDeferral(owner));
        runner.selected = owner;
        assertNull(admission.controlDeferral(owner));
        mod.user = TestObjects.allocate(User.class);
        runner.selected = mod.user;
        assertNull(admission.controlDeferral(owner));
        assertEquals(0, safety.priorityCalls);
        assertEquals(0, owner.priorityCalls);
    }

    @Test
    void currentRootIgnoresIdleAndInactiveUsersAndReadsTheCurrentReplacement() {
        Mod mod = TestObjects.allocate(Mod.class);
        Runner runner = new Runner(mod);
        AutoDepositPlanAdmission admission = admission(mod, runner);
        assertNull(admission.currentRoot());
        mod.user = TestObjects.allocate(User.class);
        Task first = new DummyTask();
        mod.user.root = first;
        assertNull(admission.currentRoot());
        mod.user.active = true;
        mod.user.idle = true;
        assertNull(admission.currentRoot());
        mod.user.idle = false;
        assertSame(first, admission.currentRoot());
        Task second = new DummyTask();
        mod.user.root = second;
        assertSame(second, admission.currentRoot());
    }

    private static AutoDepositPlanAdmission admission(Mod mod, Runner runner) {
        // Null execution dependencies deliberately make any accidental planning fail these gate tests.
        return new AutoDepositPlanAdmission(mod, runner, new DepositAllAutoConflictGuard(), null, null);
    }

    private static final class Mod extends AltoClef {
        private User user;
        private Defense defense;
        private boolean failIfDefenseRead;
        @Override public UserTaskChain getUserTaskChain() { return user; }
        @Override public MobDefenseChain getMobDefenseChain() {
            if (failIfDefenseRead) throw new AssertionError("Manual storage did not precede safety observation");
            return defense;
        }
    }

    private static final class Runner extends TaskRunner {
        private TaskChain selected;
        private Runner(AltoClef mod) { super(mod); }
        @Override public TaskChain getCurrentTaskChain() { return selected; }
    }

    private static final class User extends UserTaskChain {
        private boolean active;
        private boolean idle;
        private Task root;
        private User() { super(null); }
        @Override public boolean isActive() { return active; }
        @Override public boolean isRunningIdleTask() { return idle; }
        @Override public Task getCurrentTask() { return root; }
    }

    private static final class Defense extends MobDefenseChain {
        private Defense() { super(null); }
        @Override public boolean isToolInputClaimed() { return true; }
        @Override public float getPriority() { throw new AssertionError("Admission reevaluated defense priority"); }
    }

    private static final class PassiveChain extends TaskChain {
        private int priorityCalls;
        private PassiveChain(TaskRunner runner) { super(runner); }
        @Override protected void onStop() { }
        @Override public void onInterrupt(TaskChain other) { }
        @Override protected void onTick() { }
        @Override public float getPriority() { priorityCalls++; return 100; }
        @Override public boolean isActive() { return true; }
        @Override public String getName() { return "native selection fixture"; }
    }

    private static final class DummyTask extends Task {
        @Override protected void onStart() { }
        @Override protected Task onTick() { return null; }
        @Override protected void onStop(Task interruptTask) { }
        @Override protected boolean isEqual(Task other) { return other == this; }
        @Override protected String toDebugString() { return "current root fixture"; }
    }
}
