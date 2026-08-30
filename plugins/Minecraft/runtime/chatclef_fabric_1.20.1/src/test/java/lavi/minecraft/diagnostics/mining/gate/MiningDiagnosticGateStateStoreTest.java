package lavi.minecraft.diagnostics.mining.gate;

import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticSessionBudget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Prove gate bookkeeping cannot grow after detail or correlation capacity is exhausted.
class MiningDiagnosticGateStateStoreTest {
    @Test
    void storeHonorsItsHardRetentionBound() {
        MiningDiagnosticGateStateStore store = new MiningDiagnosticGateStateStore(2);

        store.stateFor("one", 1, 1, true);
        store.stateFor("two", 2, 2, true);
        store.stateFor("three", 3, 3, true);

        assertEquals(2, store.trackedStateCount());
        assertTrue(store.containsState("one"));
        assertFalse(store.containsState("three"));
    }

    @Test
    void correlationCapPreventsNewBucketRetention() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticGateStateStore store = new MiningDiagnosticGateStateStore();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget, store);
        for (int index = 0; index < MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT; index++) {
            assertTrue(gate.evaluateAt(
                    "stable-bucket", "fingerprint-" + index, "correlation", false, index, index).emit);
        }
        int retainedAtCap = store.trackedStateCount();

        for (int index = 0; index < 1_000; index++) {
            assertFalse(gate.evaluateAt(
                    "new-bucket-" + index, "same", "correlation", false,
                    MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT + index,
                    MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT + index).emit);
        }

        assertEquals(retainedAtCap, store.trackedStateCount());
    }

    @Test
    void sessionCapPreventsNewBucketRetention() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticGateStateStore store = new MiningDiagnosticGateStateStore();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget, store);
        for (int index = 0; index < MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT; index++) {
            String correlation = "correlation-" + (index / MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
            assertTrue(gate.evaluateAt(
                    "stable-" + correlation, "fingerprint-" + index, correlation, false, index, index).emit);
        }
        int retainedAtCap = store.trackedStateCount();
        assertTrue(gate.evaluateAt("trigger-cap", "same", "overflow", false,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT).reportSessionCap);

        for (int index = 0; index < 1_000; index++) {
            assertFalse(gate.evaluateAt(
                    "overflow-bucket-" + index, "same", "overflow-" + index, false,
                    MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + index + 1L,
                    MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + index + 1L).emit);
        }

        assertEquals(retainedAtCap, store.trackedStateCount());
    }
}
