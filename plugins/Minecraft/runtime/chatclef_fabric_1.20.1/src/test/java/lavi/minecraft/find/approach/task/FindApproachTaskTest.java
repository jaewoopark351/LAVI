package lavi.minecraft.find.approach.task;

import java.util.concurrent.atomic.AtomicLong;
import lavi.minecraft.find.approach.movement.FindApproachMovementPort;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.observation.FindObservationOperation;
import lavi.minecraft.find.observation.FindObservationPort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Test authoritative parent deadline, candidate revalidation and assigned-once terminal.
class FindApproachTaskTest {
    static class Observations implements FindObservationPort {
        final Binding binding = new Binding(new Object(), new Object(), "minecraft:overworld", 0, 64, 0, -64, 320);
        final FindCandidate candidate = new FindCandidate(12, "digest", "uuid", 20, 64, 0, 400);
        boolean valid = true, worldMatches = true, catalogMatches = true;
        Runnable validationHook = () -> { };
        public Binding binding() { return binding; }
        public boolean matches(Binding original) { return worldMatches && original == binding; }
        public boolean resourceBindingMatches(FindRequest request) { return catalogMatches; }
        public EntityScan scanEntities(FindRequest request, Binding original, int limit) { return new EntityScan(1, 1, true, candidate); }
        public FindCandidate readBlock(FindRequest request, Binding original, int x, int y, int z) { return null; }
        public FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate) {
            validationHook.run(); return valid ? candidate : null;
        }
    }
    static class Movement implements FindApproachMovementPort {
        int begins, ticks, releases;
        Step next = Step.waiting();
        Runnable tickHook = () -> { }, cleanupHook = () -> { };
        public void begin(FindObservationPort.Binding binding, FindRequest request, FindCandidate candidate) { begins++; }
        public Step tick() { ticks++; tickHook.run(); return next; }
        public void suspend() { releases++; cleanupHook.run(); }
        public boolean quiet() { return true; }
    }
    FindApproachTask task(Observations port, Movement movement, AtomicLong clock) {
        var request = new FindRequest("entity", "minecraft:villager", "approach", "catalog", 1);
        var log = new FindLog("op", (event, fields) -> { });
        var observation = new FindObservationOperation(request, "op", port, clock::get, log);
        return new FindApproachTask(observation, port, movement, clock::get, log);
    }
    @Test void preemptionWithNullInterruptDoesNotTerminalizeOrResetOriginalDeadline() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock);
        task.onStart(); task.onTick(); task.onStop(null);
        assertNull(task.outcome()); assertEquals(1, movement.begins);
        clock.set(FindApproachTask.DEADLINE_NANOS + 1);
        task.onStart(); task.onTick();
        assertEquals("TIMEOUT", task.outcome().findResult());
        assertFalse(task.outcome().satisfied()); assertEquals(1, movement.begins);
    }
    @Test void sameTargetMustRevalidateAfterMovementCleanupBeforeSuccess() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        port.valid = false;
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        task.onTick();
        assertEquals("TARGET_LOST", task.outcome().findResult()); assertNull(task.outcome().candidate());
        assertTrue(movement.releases > 0); assertFalse(task.outcome().satisfied());
    }
    @Test void worldReplacementPreventsAnotherMovementTickAndLateResultCannotReplaceTerminal() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        port.worldMatches = false; int before = movement.ticks;
        task.onTick(); var terminal = task.outcome();
        assertEquals("INTERRUPTED", terminal.findResult()); assertEquals(before, movement.ticks);
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        task.onStop(null); task.onStart(); task.onTick();
        assertSame(terminal, task.outcome()); assertEquals(before, movement.ticks);
    }
    @Test void nativeFailureReasonsArePreservedWithNoNewPlanOrAcquisition() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        movement.next = new FindApproachMovementPort.Step("native_step_no_progress_timeout", false, null);
        task.onTick();
        assertEquals("TIMEOUT", task.outcome().findResult());
        assertEquals("native_step_no_progress_timeout", task.outcome().reason());
        assertEquals(1, movement.begins);
    }
    @Test void expirationInsideNativeTickCannotCommitLateArrival() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        movement.tickHook = () -> clock.set(FindApproachTask.DEADLINE_NANOS);
        task.onTick();
        assertEquals("TIMEOUT", task.outcome().findResult());
        assertFalse(task.outcome().satisfied()); assertNull(task.outcome().candidate());
    }
    @Test void expirationDuringOwnedCleanupCannotCommitLateArrival() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        movement.cleanupHook = () -> clock.set(FindApproachTask.DEADLINE_NANOS);
        task.onTick();
        assertEquals("TIMEOUT", task.outcome().findResult());
        assertFalse(task.outcome().satisfied()); assertNull(task.outcome().candidate());
    }
    @Test void expirationDuringFinalRevalidationCannotCommitLateArrival() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        port.validationHook = () -> clock.set(FindApproachTask.DEADLINE_NANOS);
        task.onTick();
        assertEquals("TIMEOUT", task.outcome().findResult());
        assertFalse(task.outcome().satisfied()); assertNull(task.outcome().candidate());
    }
    @Test void progressSnapshotFailureCannotChangeAssignedSuccess() {
        var port = new Observations(); var clock = new AtomicLong();
        var movement = new Movement() {
            @Override public Progress progress() { throw new IllegalStateException("diagnostic_snapshot_failure"); }
        };
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        task.onTick();
        assertEquals("FOUND_AND_IN_SAFE_RANGE", task.outcome().findResult());
        assertTrue(task.outcome().satisfied());
    }
    @Test void resourceReloadDuringMovementInterruptsBeforeAnotherNativeTick() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        port.catalogMatches = false; int before = movement.ticks;
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        task.onTick();
        assertEquals("INTERRUPTED", task.outcome().findResult()); assertEquals(before, movement.ticks);
        assertFalse(task.outcome().satisfied()); assertNull(task.outcome().candidate()); assertTrue(movement.releases > 0);
    }
    @Test void resourceReloadInsideFinalCandidateRevalidationCannotCommitSuccess() {
        var port = new Observations(); var movement = new Movement(); var clock = new AtomicLong();
        var task = task(port, movement, clock); task.onStart(); task.onTick();
        movement.next = new FindApproachMovementPort.Step("FOUND_AND_IN_SAFE_RANGE", true, port.candidate);
        port.validationHook = () -> port.catalogMatches = false;
        task.onTick();
        assertEquals("INTERRUPTED", task.outcome().findResult());
        assertFalse(task.outcome().satisfied()); assertNull(task.outcome().candidate()); assertTrue(movement.releases > 0);
    }
}
