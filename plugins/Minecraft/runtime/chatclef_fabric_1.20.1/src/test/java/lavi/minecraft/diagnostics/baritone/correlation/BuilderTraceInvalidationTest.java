package lavi.minecraft.diagnostics.baritone.correlation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: A worker retaining an old ledger cannot rebuild state after diagnostic cleanup.
final class BuilderTraceInvalidationTest {
    @Test void invalidatedLedgerRejectsLateWorkerHistoryAndAdoption() {
        BuilderTraceLedger ledger = new BuilderTraceLedger();
        ledger.record("before-off");
        ledger.invalidate();
        ledger.record("late-worker");
        ledger.recordIfChanged("late", "late-worker");
        ledger.adopt(new Object(), new Object());
        assertEquals(0, ledger.historySize());
        assertEquals(0, ledger.links());
        ledger.clear();
        ledger.record("late-after-clear");
        assertEquals(0, ledger.historySize());
    }
}
