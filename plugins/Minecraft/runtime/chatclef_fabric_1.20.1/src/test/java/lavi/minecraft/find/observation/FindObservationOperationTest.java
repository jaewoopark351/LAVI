//#if MC == 12001
package lavi.minecraft.find.observation;

import lavi.minecraft.find.observation.FindObservationPort.EntityScan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Test bounded full-scope observation, target loss and preemption without live-world actions.
class FindObservationOperationTest {
    private static FindCandidate candidate(int x) { return new FindCandidate(1, "digest", "uuid", x, 1, 1, x * x); }
    private static final class Port implements FindObservationPort {
        Binding binding = new Binding(new Object(), new Object(), "minecraft:overworld", 0, 1, 0, -64, 320);
        EntityScan scan = new EntityScan(10, 1, true, candidate(2));
        boolean matches = true;
        boolean resourceMatches = true, reloadDuringRevalidation;
        int reads, scans;
        FindCandidate revalidated = candidate(-3);
        AtomicLong clock;
        long revalidationDelay;
        boolean bindingThrows, blockThrows, revalidationThrows;
        @Override public Binding binding() { if (bindingThrows) throw new IllegalStateException("read_failed"); return binding; }
        @Override public boolean matches(Binding ignored) { return matches; }
        @Override public boolean resourceBindingMatches(FindRequest request) { return resourceMatches; }
        @Override public EntityScan scanEntities(FindRequest request, Binding ignored, int limit) { scans++; return scan; }
        @Override public FindCandidate readBlock(FindRequest request, Binding ignored, int x, int y, int z) { reads++; if (blockThrows) throw new IllegalStateException("read_failed"); return reads == 1 ? candidate(2) : null; }
        @Override public FindCandidate revalidate(FindRequest request, Binding ignored, FindCandidate candidate) {
            if (reloadDuringRevalidation) resourceMatches = false;
            if (revalidationThrows) throw new IllegalStateException("read_failed");
            if (clock != null) clock.addAndGet(revalidationDelay);
            return revalidated;
        }
    }
    private static FindObservationOperation operation(String kind, Port port, AtomicLong clock, List<String> output) {
        return new FindObservationOperation(new FindRequest(kind, "minecraft:villager", "report", "catalog", 1), "operation",
                port, clock::get, new FindLog("operation", (event, fields) -> output.add(event)));
    }
    @Test void fullScopeFoundUsesTerminalLocationAndCommitsOnlyOnce() {
        Port port = new Port(); AtomicLong clock = new AtomicLong(); List<String> output = new ArrayList<>();
        var operation = operation("entity", port, clock, output);
        operation.tick(); var result = operation.outcome();
        assertTrue(result.satisfied()); assertTrue(result.scopeComplete());
        assertEquals("FOUND_AND_REPORTED", result.findResult()); assertEquals(-3, result.candidate().x());
        operation.suspend(); operation.start(); operation.tick();
        assertSame(result, operation.outcome()); assertEquals(1, port.scans);
        assertEquals(1, output.stream().filter("REPORT_TERMINAL"::equals).count());
    }
    @Test void aCandidateCannotPromoteIncompleteEntityEnumeration() {
        Port port = new Port(); port.scan = new EntityScan(4096, 3, false, candidate(1));
        var operation = operation("entity", port, new AtomicLong(), new ArrayList<>());
        operation.tick();
        assertEquals("OBSERVATION_BOUNDS_EXHAUSTED", operation.outcome().findResult());
        assertFalse(operation.outcome().satisfied()); assertNull(operation.outcome().candidate());
        assertFalse(operation.outcome().scopeComplete()); assertEquals(4096, operation.outcome().visited());
    }
    @Test void noMatchRequiresCompleteEnumeration() {
        Port port = new Port(); port.scan = new EntityScan(7, 0, true, null);
        var operation = operation("entity", port, new AtomicLong(), new ArrayList<>());
        operation.tick();
        assertEquals("NOT_OBSERVED_IN_LOADED_SCOPE", operation.outcome().findResult());
        assertFalse(operation.outcome().satisfied()); assertTrue(operation.outcome().scopeComplete());
    }
    @Test void suspendedBlockSearchKeepsOriginalDeadlineAndCounter() {
        Port port = new Port(); AtomicLong clock = new AtomicLong();
        var operation = operation("block", port, clock, new ArrayList<>());
        operation.tick(); int before = port.reads;
        assertEquals(4096, before); assertNull(operation.outcome());
        operation.suspend(); clock.set(5_000_000_000L); operation.start(); operation.tick();
        assertEquals(before, port.reads); assertEquals(before, operation.outcome().visited());
        assertEquals("OBSERVATION_BOUNDS_EXHAUSTED", operation.outcome().findResult()); assertNull(operation.outcome().candidate());
    }
    @Test void blockCursorFinishesWithinTotalLimitAndPreservesCandidateUntilFullScope() {
        Port port = new Port(); var operation = operation("block", port, new AtomicLong(), new ArrayList<>());
        operation.tick(); assertNull(operation.outcome());
        for (int tick = 0; tick < 70 && operation.outcome() == null; tick++) operation.tick();
        assertNotNull(operation.outcome()); assertTrue(operation.outcome().satisfied());
        assertEquals(65 * 65 * 65, port.reads); assertEquals(port.reads, operation.outcome().visited());
    }
    @Test void changingWorldBindingFailsWithoutClaimingCandidateLocation() {
        Port port = new Port(); var operation = operation("block", port, new AtomicLong(), new ArrayList<>());
        operation.tick(); port.matches = false; operation.tick();
        assertEquals("INTERRUPTED", operation.outcome().findResult()); assertNull(operation.outcome().candidate());
    }
    @Test void selectedTargetMustStillExistAtTerminal() {
        Port port = new Port(); port.revalidated = null;
        var operation = operation("entity", port, new AtomicLong(), new ArrayList<>()); operation.tick();
        assertEquals("TARGET_LOST", operation.outcome().findResult()); assertFalse(operation.outcome().satisfied());
    }
    @Test void deadlineExpiringDuringRevalidationCannotCommitFound() {
        Port port = new Port(); AtomicLong clock = new AtomicLong(); port.clock = clock; port.revalidationDelay = 5_000_000_000L;
        var operation = operation("entity", port, clock, new ArrayList<>()); operation.tick();
        assertEquals("OBSERVATION_BOUNDS_EXHAUSTED", operation.outcome().findResult()); assertNull(operation.outcome().candidate());
    }
    @Test void throwingLogSinkDoesNotAlterFoundOutcome() {
        Port port = new Port(); var operation = new FindObservationOperation(new FindRequest("entity", "minecraft:villager", "report", "digest", 1),
                "operation", port, () -> 0, new FindLog("operation", (event, fields) -> { throw new IllegalStateException("sink_failed"); }));
        operation.tick(); assertTrue(operation.outcome().satisfied());
    }
    @Test void initializationReadFailureCommitsTypedFailureAndCannotRestart() {
        Port port = new Port(); port.bindingThrows = true;
        var operation = operation("entity", port, new AtomicLong(), new ArrayList<>());
        assertDoesNotThrow(operation::tick); var outcome = operation.outcome();
        assertEquals("INTERNAL_ERROR", outcome.findResult()); assertNull(outcome.candidate());
        port.bindingThrows = false; operation.start(); operation.tick();
        assertSame(outcome, operation.outcome()); assertEquals(0, port.scans);
    }
    @Test void blockReadFailureCommitsActualVisitedCountWithoutClaimingFound() {
        Port port = new Port(); port.blockThrows = true;
        var operation = operation("block", port, new AtomicLong(), new ArrayList<>());
        assertDoesNotThrow(operation::tick);
        assertEquals("INTERNAL_ERROR", operation.outcome().findResult());
        assertEquals(1, operation.outcome().visited()); assertFalse(operation.outcome().scopeComplete()); assertNull(operation.outcome().candidate());
    }
    @Test void revalidationFailureCannotPromotePreviouslyObservedCandidate() {
        Port port = new Port(); port.revalidationThrows = true;
        var operation = operation("entity", port, new AtomicLong(), new ArrayList<>());
        assertDoesNotThrow(operation::tick);
        assertEquals("INTERNAL_ERROR", operation.outcome().findResult());
        assertTrue(operation.outcome().scopeComplete()); assertNull(operation.outcome().candidate());
    }
    @Test void resourceReloadInterruptsSuspendedSearchWithoutAnotherWorldRead() {
        Port port = new Port(); var operation = operation("block", port, new AtomicLong(), new ArrayList<>());
        operation.tick(); int before = port.reads; operation.suspend(); port.resourceMatches = false;
        operation.start(); operation.tick();
        assertEquals("INTERRUPTED", operation.outcome().findResult()); assertEquals(before, port.reads);
        assertEquals(before, operation.outcome().visited()); assertNull(operation.outcome().candidate());
    }
    @Test void reloadAtFinalRevalidationCannotPublishFoundCoordinates() {
        Port port = new Port(); port.reloadDuringRevalidation = true;
        var operation = operation("entity", port, new AtomicLong(), new ArrayList<>()); operation.tick();
        assertEquals("INTERRUPTED", operation.outcome().findResult()); assertNull(operation.outcome().candidate());
        assertEquals("catalog_resource_binding_changed", operation.outcome().reason());
    }
}
//#endif
