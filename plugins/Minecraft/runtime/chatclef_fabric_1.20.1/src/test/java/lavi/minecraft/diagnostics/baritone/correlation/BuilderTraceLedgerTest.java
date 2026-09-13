package lavi.minecraft.diagnostics.baritone.correlation;

import lavi.minecraft.diagnostics.baritone.builder.BuilderPathSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuilderTraceLedgerTest {
    private static BuilderPathProvenance origin(long generation) {
        return new BuilderPathProvenance(generation, "request-" + generation, "cached-task", "goal",
                "POST_PROCESS_RETURN", "none", "none", BuilderPathSnapshot.capture(null));
    }
    @Test void currentAndNextRemainBoundAfterGeneralLinkEvictionAndPromotion() {
        BuilderTraceLedger ledger = new BuilderTraceLedger();
        Object current = new Object(), next = new Object();
        BuilderPathProvenance first = origin(1), second = origin(2);
        ledger.bind(current, first); ledger.bind(next, second); ledger.adopt(current, next);
        for (int i = 0; i < 1000; i++) ledger.bind(new Object(), origin(3 + i));
        assertEquals(BuilderTraceLedger.LINK_CAP, ledger.links());
        assertSame(first, ledger.find(current));
        assertSame(second, ledger.find(next));
        ledger.adopt(next, null);
        assertSame(second, ledger.find(next));
        assertNull(ledger.find(current));
        assertTrue(ledger.evictions() > 0);
    }
    @Test void keysUseReferenceIdentityRatherThanEngineEquality() {
        BuilderTraceLedger ledger = new BuilderTraceLedger();
        Object first = new String("same"), second = new String("same");
        ledger.bind(first, origin(1)); ledger.bind(second, origin(2));
        ledger.remove(first);
        assertNull(ledger.find(first));
        assertEquals(2, ledger.find(second).calculation());
    }
    @Test void recentHistoryContinuesWithoutAnOutputSinkAndClearsOnTeardown() {
        BuilderTraceLedger ledger = new BuilderTraceLedger();
        Object path = new Object(); ledger.bind(path, origin(1)); ledger.adopt(path, null);
        for (int i = 0; i < 10000; i++) ledger.record("transition-" + i);
        assertEquals(32, ledger.historySize());
        assertTrue(ledger.recent().contains("transition-9999"));
        assertFalse(ledger.recent().contains("transition-0;"));
        assertEquals(1, ledger.find(path).calculation());
        ledger.clear();
        assertNull(ledger.find(path)); assertEquals(0, ledger.historySize()); assertEquals(0, ledger.links());
    }
    @Test void twoInputTransformationPreservesOriginalRequestRatherThanCurrentCommand() {
        BuilderPathProvenance before = origin(7);
        BuilderPathProvenance after = before.transformed("SPLICE_CREATED", "path-a", "path-b", BuilderPathSnapshot.capture(null));
        assertEquals(7, after.calculation()); assertEquals("request-7", after.request());
        assertEquals("path-a", after.firstInput()); assertEquals("path-b", after.secondInput());
    }
    @Test void interleavedUnchangedObservationsCannotEraseEarlierTransitions() {
        BuilderTraceLedger ledger = new BuilderTraceLedger();
        ledger.record("ASSEMBLY_RETURN_PARTIAL G=1 PC=1 MC=0");
        for (int i = 0; i < 10000; i++) {
            ledger.recordIfChanged("cutoff:same", "SAME_EXECUTOR originalP=abc I=0");
            ledger.recordIfChanged("splice:same", "SAME_EXECUTOR returnedP=abc I=0");
        }
        assertEquals(3, ledger.historySize());
        assertTrue(ledger.recent().contains("ASSEMBLY_RETURN_PARTIAL"));
    }
}
