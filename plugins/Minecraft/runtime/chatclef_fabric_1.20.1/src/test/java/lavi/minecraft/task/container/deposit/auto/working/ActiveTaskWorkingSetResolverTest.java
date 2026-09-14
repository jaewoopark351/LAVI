package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260914_kpopmodder: Accept only the selected automatic caller and the still-owned current user tree.
class ActiveTaskWorkingSetResolverTest {
    @Test
    void legacyEntryStillRequiresNativeUserSelection() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = fixture.automatic;
        assertEquals("user_task_chain_not_selected", fixture.resolver.resolve(fixture.mod).reason());
        fixture.runner.selected = fixture.user;
        assertEquals("world_or_player_missing", fixture.resolver.resolve(fixture.mod).reason());
    }

    @Test
    void exactSelectedAutomaticOwnerPassesSelectionWithoutWeakeningWorldValidation() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = fixture.automatic;
        assertEquals("world_or_player_missing", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
    }

    @Test
    void unrelatedSafetySelectionCannotUseAutomaticSelectionPermission() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = new SelectedChain(fixture.runner);
        assertEquals("user_task_chain_not_selected", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
    }

    @Test
    void replacementRootCannotReuseThePreviousRootsCachedPath() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = fixture.automatic;
        fixture.user.root = new DummyTask();
        assertEquals("stale_user_task_path", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
    }

    @Test
    void oldDisconnectedDescendantCannotContributeReservations() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = fixture.automatic;
        fixture.user.path = List.of(fixture.user.root, new DummyTask());
        assertEquals("stale_user_task_path", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
    }

    @Test
    void stoppedRootCannotUseItsStillCachedPath() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = fixture.automatic;
        fixture.user.root.stopped = true;
        assertEquals("user_task_root_stopped", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
    }

    @Test
    void idleAndInactiveUserRootsRemainOutsideWorkingSetAdmission() {
        Fixture fixture = new Fixture();
        fixture.runner.selected = fixture.automatic;
        fixture.user.idle = true;
        assertEquals("no_active_non_idle_user_task", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
        fixture.user.idle = false;
        fixture.user.active = false;
        assertEquals("no_active_non_idle_user_task", fixture.resolver.resolve(fixture.mod, fixture.automatic).reason());
    }

    private static final class Fixture {
        private final Mod mod = TestObjects.allocate(Mod.class);
        private final Runner runner = new Runner(mod);
        private final User user = TestObjects.allocate(User.class);
        private final SelectedChain automatic = new SelectedChain(runner);
        private final ActiveTaskWorkingSetResolver resolver = new ActiveTaskWorkingSetResolver();
        private Fixture() {
            mod.runner = runner;
            mod.user = user;
            user.active = true;
            user.root = new DummyTask();
            user.path = List.of(user.root);
        }
    }

    private static final class Mod extends AltoClef {
        private Runner runner;
        private User user;
        @Override public TaskRunner getTaskRunner() { return runner; }
        @Override public UserTaskChain getUserTaskChain() { return user; }
        @Override public ClientWorld getWorld() { return null; }
        @Override public ClientPlayerEntity getPlayer() { return null; }
    }

    private static final class Runner extends TaskRunner {
        private TaskChain selected;
        private Runner(AltoClef mod) { super(mod); }
        @Override public TaskChain getCurrentTaskChain() { return selected; }
    }

    private static final class User extends UserTaskChain {
        private boolean active;
        private boolean idle;
        private DummyTask root;
        private List<Task> path;
        private User() { super(null); }
        @Override public boolean isActive() { return active; }
        @Override public boolean isRunningIdleTask() { return idle; }
        @Override public Task getCurrentTask() { return root; }
        @Override public List<Task> getTasks() { return path; }
    }

    private static final class SelectedChain extends TaskChain {
        private SelectedChain(TaskRunner runner) { super(runner); }
        @Override protected void onStop() { }
        @Override public void onInterrupt(TaskChain other) { }
        @Override protected void onTick() { }
        @Override public float getPriority() { throw new AssertionError("Working-set observation evaluated priority"); }
        @Override public boolean isActive() { return true; }
        @Override public String getName() { return "selected automatic fixture"; }
    }

    private static final class DummyTask extends Task {
        private boolean stopped;
        @Override public boolean stopped() { return stopped; }
        @Override protected void onStart() { }
        @Override protected Task onTick() { return null; }
        @Override protected void onStop(Task interruptTask) { }
        @Override protected boolean isEqual(Task other) { return other == this; }
        @Override protected String toDebugString() { return "working-set owner fixture"; }
    }
}
