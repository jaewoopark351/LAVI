//#if MC == 12001
package lavi.minecraft.find.exploration.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.find.approach.movement.FindApproachMovementPort;
import lavi.minecraft.find.command.FindNativeCompletionObserver;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.find.exploration.movement.FindExplorationMovementPort;
import lavi.minecraft.find.exploration.operation.FindExplorationLimits;
import lavi.minecraft.find.exploration.operation.FindExplorationOperation;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.observation.FindObservationPort;
import lavi.minecraft.find.observation.FindObservationPort.Binding;
import lavi.minecraft.find.result.FindOutcome;
import lavi.minecraft.find.result.FindTaskResultSource;
import lavi.minecraft.integration.lifecycle.root.UserRootLifetime;
import lavi.minecraft.integration.lifecycle.root.UserRootOwnership;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: A retained Task identity cannot authorize work belonging to another callback lifetime.
class FindExplorationLifetimeBindingTest {
    private static final class Chain extends UserTaskChain {
        Task task;
        UserRootOwnership ownership;
        Chain() { super(null); }
        @Override public Task getCurrentTask() { return task; }
        @Override public UserRootLifetime currentRootLifetime() { return ownership.currentFor(task); }
    }
    private static final class Mod extends AltoClef {
        Chain chain;
        ClientWorld world;
        ClientPlayerEntity player;
        @Override public UserTaskChain getUserTaskChain() { return chain; }
        @Override public ClientWorld getWorld() { return world; }
        @Override public ClientPlayerEntity getPlayer() { return player; }
    }
    private static final class Reads implements FindObservationPort {
        final Binding position = new Binding(new Object(), new Object(), "minecraft:overworld", 0, 65, 0, -64, 320);
        int scans;
        @Override public Binding binding() { return position; }
        @Override public boolean matches(Binding binding) { return binding == position; }
        @Override public EntityScan scanEntities(FindRequest request, Binding binding, int limit) {
            scans++; return new EntityScan(1, 0, true, null);
        }
        @Override public FindCandidate readBlock(FindRequest request, Binding binding, int x, int y, int z) { return null; }
        @Override public FindCandidate revalidate(FindRequest request, Binding binding, FindCandidate candidate) { return candidate; }
    }
    private static final class Explore implements FindExplorationMovementPort {
        int starts, releases;
        boolean active;
        @Override public void begin(Binding admission, java.util.function.BooleanSupplier budget) { starts++; active = true; }
        @Override public Step tick() { return new Step(null, false, false, null); }
        @Override public void suspend() { releases++; active = false; }
        @Override public boolean quiet() { return !active; }
    }
    private static final class Approach implements FindApproachMovementPort {
        @Override public void begin(Binding binding, FindRequest request, FindCandidate candidate) { fail("report must not approach"); }
        @Override public Step tick() { fail("report must not approach"); return null; }
        @Override public void suspend() { }
        @Override public boolean quiet() { return true; }
    }
    private static final class SourceTask extends Task implements FindTaskResultSource {
        UserRootLifetime bound;
        int observed, retired;
        @Override public void bindUserRootLifetime(UserRootLifetime lifetime) { bound = lifetime; }
        @Override public void observeClientTick() { observed++; }
        @Override public void retireOwnedResources(String reason) { retired++; }
        @Override public FindRequest request() { return new FindRequest("entity", "minecraft:villager", "report", "", 0); }
        @Override public String operationId() { return "op"; }
        @Override public FindOutcome outcome() { return null; }
        @Override public void diagnosticRetired(String reason) { }
        @Override public void suppressNativePresentation() { }
        @Override public boolean nativePresentationSuppressed() { return false; }
        @Override protected void onStart() { }
        @Override protected Task onTick() { return null; }
        @Override protected void onStop(Task interrupt) { }
        @Override public boolean isFinished() { return false; }
        @Override protected boolean isEqual(Task task) { return task == this; }
        @Override protected String toDebugString() { return "lifetime fixture"; }
    }
    private static final class Fixture implements AutoCloseable {
        final Field modSingleton, clientSingleton;
        final Object previousMod, previousClient;
        final Mod mod;
        final Chain chain;
        final UserRootOwnership ownership = new UserRootOwnership();
        final FindNativeCompletionObserver observer = new FindNativeCompletionObserver();
        Fixture() throws ReflectiveOperationException {
            SharedConstants.createGameVersion(); Bootstrap.initialize();
            mod = TestObjects.allocate(Mod.class);
            chain = TestObjects.allocate(Chain.class); chain.ownership = ownership; mod.chain = chain;
            var client = TestObjects.allocate(MinecraftClient.class);
            client.world = TestObjects.allocate(ClientWorld.class); client.player = TestObjects.allocate(ClientPlayerEntity.class);
            mod.world = client.world; mod.player = client.player;
            modSingleton = AltoClef.class.getDeclaredField("instance"); modSingleton.setAccessible(true);
            clientSingleton = MinecraftClient.class.getDeclaredField("instance"); clientSingleton.setAccessible(true);
            previousMod = modSingleton.get(null); previousClient = clientSingleton.get(null);
            modSingleton.set(null, mod); clientSingleton.set(null, client);
        }
        UserRootLifetime assign(Task task, Object callback) {
            chain.task = task; ownership.assigned(task, callback, mod.world, mod.player);
            return chain.currentRootLifetime();
        }
        @Override public void close() throws IllegalAccessException {
            try { observer.retire("fixture_finished"); }
            finally { modSingleton.set(null, previousMod); clientSingleton.set(null, previousClient); }
        }
    }
    private static FindExplorationTask task(Reads reads, Explore explore, FindLog log) {
        var task = TestObjects.allocate(FindExplorationTask.class);
        var operation = new FindExplorationOperation(new FindRequest("entity", "minecraft:villager", "report", "", 0),
                "00000000-0000-0000-0000-000000000011", task, reads, explore, new Approach(), () -> 0, log, 0,
                FindExplorationLimits.DEFAULT);
        TestObjects.setField(task, FindExplorationTask.class, "operation", operation);
        TestObjects.setField(task, FindExplorationTask.class, "log", log);
        return task;
    }

    @Test void sameTaskWithReplacedCallbackCannotResumeOrObserveAnotherLifetime() throws Exception {
        try (var fixture = new Fixture()) {
            var reads = new Reads(); var explore = new Explore();
            var task = task(reads, explore, new FindLog("lifetime", (event, values) -> { }));
            var first = fixture.assign(task, new Object()); task.bindUserRootLifetime(first);
            task.onStart(); task.onTick(); assertEquals(1, reads.scans); assertFalse(task.isFinished());
            var replaced = fixture.assign(task, new Object()); assertNotSame(first, replaced);
            assertSame(task, fixture.chain.getCurrentTask());
            task.onTick(); assertTrue(task.isFinished()); assertNull(task.outcome());
            assertEquals(1, reads.scans); assertEquals(0, explore.starts);
            task.bindUserRootLifetime(replaced); task.onStart(); task.onTick();
            assertEquals(1, reads.scans); assertNull(task.outcome());
        }
    }

    @Test void bindingOutputPreservesFirstLifetimeAndRejectsForeignRebindingWithoutNewWork() throws Exception {
        try (var fixture = new Fixture()) {
            ChatClefDiagnostics.setBoundaryEnabled(false); ChatClefDiagnostics.resetDiagnosticSessionForTests();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            PrintStream previous = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FindLog bindingLog = null;
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                var reads = new Reads(); var explore = new Explore();
                bindingLog = new FindLog("00000000-0000-0000-0000-000000000011");
                var task = task(reads, explore, bindingLog);
                var first = fixture.assign(task, new Object());
                task.bindUserRootLifetime(first); task.bindUserRootLifetime(first);
                var foreign = fixture.assign(task, new Object()); task.bindUserRootLifetime(foreign);
                assertTrue(task.isFinished()); assertNull(task.outcome()); assertEquals(0, reads.scans);
                var field = FindExplorationTask.class.getDeclaredField("rootLifetime"); field.setAccessible(true);
                assertSame(first, field.get(task));
                bindingLog.close("fixture_finished");
                assertEquals(0, ChatClefDiagnostics.diagnosticSessionSnapshot().emissionPending());
                assertEquals(0, ChatClefDiagnostics.diagnosticSessionSnapshot().emissionInProgress());
                String output = bytes.toString(StandardCharsets.UTF_8);
                for (String reason : new String[]{"accepted_first_binding", "already_same_binding", "rejected_rebinding"})
                    assertTrue(output.lines().anyMatch(line -> line.contains("event=FIND_ROOT_LIFETIME_BINDING_RESULT")
                            && line.contains("reason=" + reason)), output);
                assertFalse(output.contains(first.toString()), output);
                assertTrue(output.contains("event=FIND_TRACE_TERMINAL"), output);
            } finally {
                try { if (bindingLog != null) bindingLog.close("fixture_finished"); }
                finally { System.setOut(previous); ChatClefDiagnostics.setBoundaryEnabled(false); }
            }
        }
    }

    @Test void newFindCommandsDoNotCollapseEqualRequestsIntoThePreviousOperation() {
        var left = task(new Reads(), new Explore(), new FindLog("left", (event, values) -> { }));
        var right = task(new Reads(), new Explore(), new FindLog("right", (event, values) -> { }));
        assertEquals(left.request(), right.request()); assertNotEquals(left, right); assertEquals(left, left);
    }

    @Test void nativeObserverForwardsBindingAndStopsOnSameTaskForeignLifetime() throws Exception {
        try (var fixture = new Fixture()) {
            var task = new SourceTask(); var first = fixture.assign(task, new Object());
            fixture.observer.prepare(task, task, fixture.mod); fixture.observer.bind(first);
            assertSame(first, task.bound); fixture.observer.onEndClientTick(); assertEquals(1, task.observed);
            fixture.assign(task, new Object()); fixture.observer.onEndClientTick();
            assertEquals(1, task.observed); assertEquals(1, task.retired);
            fixture.observer.onEndClientTick(); assertEquals(1, task.retired);
        }
    }
}
//#endif
