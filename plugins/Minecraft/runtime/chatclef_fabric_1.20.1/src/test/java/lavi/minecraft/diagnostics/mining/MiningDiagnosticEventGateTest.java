package lavi.minecraft.diagnostics.mining;

import lavi.minecraft.diagnostics.mining.budget.MiningDiagnosticSessionBudget;
import lavi.minecraft.diagnostics.mining.gate.MiningDiagnosticEventGate;
import lavi.minecraft.diagnostics.mining.gate.MiningDiagnosticGateDecision;
import lavi.minecraft.diagnostics.mining.gate.MiningDiagnosticGateStateStore;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Lock generic mining dedupe and bounded-session policy without a live client.
class MiningDiagnosticEventGateTest {
    @Test
    void detailCapIs256PerCorrelationAndIndependentAcrossCorrelations() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget);

        for (int index = 0; index < 256; index++) {
            MiningDiagnosticGateDecision decision = gate.evaluateAt(
                    "bucket", "fingerprint-" + index, "correlation-a", false, index, index);
            assertTrue(decision.emit, "detail " + index + " should be admitted");
        }

        MiningDiagnosticGateDecision rejected = gate.evaluateAt(
                "bucket", "fingerprint-256", "correlation-a", false, 256, 256);
        assertFalse(rejected.emit);
        assertTrue(rejected.reportCorrelationCap);
        assertFalse(rejected.reportSessionCap);
        assertFalse(budget.snapshot().sessionCapSignalClaimed());

        MiningDiagnosticGateDecision independent = gate.evaluateAt(
                "bucket", "correlation-b-first", "correlation-b", false, 257, 257);
        assertTrue(independent.emit);
        assertEquals(258, budget.snapshot().detailEmissions());
    }

    @Test
    void unchangedFingerprintSummarizesAtTick200OrTenSecondFallback() {
        MiningDiagnosticEventGate tickGate = new MiningDiagnosticEventGate(new MiningDiagnosticSessionBudget());
        assertTrue(tickGate.evaluateAt("tick", "same", "tick-correlation", false, 0, 0).emit);
        assertFalse(tickGate.evaluateAt("tick", "same", "tick-correlation", false, 199,
                TimeUnit.MILLISECONDS.toNanos(9_999)).emit);
        MiningDiagnosticGateDecision tickSummary = tickGate.evaluateAt(
                "tick", "same", "tick-correlation", false, 200,
                TimeUnit.MILLISECONDS.toNanos(9_999));
        assertTrue(tickSummary.emit);
        assertTrue(tickSummary.summary);

        MiningDiagnosticEventGate timeGate = new MiningDiagnosticEventGate(new MiningDiagnosticSessionBudget());
        assertTrue(timeGate.evaluateAt("time", "same", "time-correlation", false, 0, 0).emit);
        MiningDiagnosticGateDecision timeSummary = timeGate.evaluateAt(
                "time", "same", "time-correlation", false, 100, TimeUnit.SECONDS.toNanos(10));
        assertTrue(timeSummary.emit);
        assertTrue(timeSummary.summary);
    }

    @Test
    void sameBucketAndFingerprintAreIndependentAcrossCorrelations() {
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(new MiningDiagnosticSessionBudget());

        assertTrue(gate.evaluateAt("shared", "same", "correlation-a", false, 1, 1).emit);
        assertTrue(gate.evaluateAt("shared", "same", "correlation-b", false, 2, 2).emit);
        assertFalse(gate.evaluateAt("shared", "same", "correlation-a", false, 3, 3).emit);
    }

    @Test
    void tickRegressionStartsAFreshBoundedSession() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticGateStateStore states = new MiningDiagnosticGateStateStore();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget, states);

        assertTrue(gate.evaluateAt("bucket", "same", "correlation", false, 100, 100).emit);
        assertEquals(1, budget.snapshot().sessionEmissions());
        assertTrue(gate.evaluateAt("bucket", "same", "correlation", false, 10, 200).emit);
        assertEquals(1, budget.snapshot().sessionEmissions());
        assertEquals(1, states.trackedStateCount());
    }

    @Test
    void explicitResetClearsDedupeAndBudgetTogether() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget);

        assertTrue(gate.evaluateAt("bucket", "same", "correlation", false, 1, 1).emit);
        assertFalse(gate.evaluateAt("bucket", "same", "correlation", false, 2, 2).emit);
        gate.resetSession();
        assertTrue(gate.evaluateAt("bucket", "same", "correlation", false, 3, 3).emit);
        assertEquals(1, budget.snapshot().sessionEmissions());
    }

    @Test
    void terminalAdmissionUsesReserveAfterDetailCapacityIsExhausted() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget);
        for (int index = 0; index < MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT; index++) {
            String correlation = "correlation-" + (index / MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
            assertTrue(gate.evaluateAt(
                    "bucket-" + correlation, "fingerprint-" + index, correlation, false, index, index).emit);
        }

        assertFalse(gate.evaluateAt("detail", "detail", "overflow", false,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT).emit);
        assertTrue(gate.evaluateAt("terminal", "stop", "terminal", true,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + 1L,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + 1L).emit);
        assertEquals(1, budget.snapshot().criticalEmissions());
    }

    @Test
    void correlationCapSummaryConsumesDetailAndLeavesTerminalReserveUntouched() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        MiningDiagnosticEventGate gate = new MiningDiagnosticEventGate(budget);
        for (int index = 0; index < MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT; index++) {
            assertTrue(gate.evaluateAt(
                    "bucket", "fingerprint-" + index, "capped", false, index, index).emit);
        }
        MiningDiagnosticGateDecision summary = gate.evaluateAt(
                "new-bucket", "summary", "capped", false,
                MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT,
                MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
        assertTrue(summary.reportCorrelationCap);
        assertEquals("ADMINISTRATIVE_DETAIL", summary.admission.reason());
        assertEquals(0, budget.snapshot().criticalEmissions());

        int remainingDetail = MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT
                - budget.snapshot().sessionEmissions();
        for (int index = 0; index < remainingDetail; index++) {
            String correlation = "filler-" + (index / MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
            assertTrue(gate.evaluateAt(
                    "fill-" + correlation, "fill-fingerprint-" + index, correlation, false,
                    MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT + index + 1L,
                    MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT + index + 1L).emit);
        }
        assertTrue(gate.evaluateAt("cap", "cap", "overflow", false,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + 1L,
                MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + 1L).reportSessionCap);
        for (int index = 0; index < MiningDiagnosticSessionBudget.RESERVED_CRITICAL_EVENTS - 1; index++) {
            assertTrue(gate.evaluateAt(
                    "terminal-" + index, "stop-" + index, "terminal-" + index, true,
                    MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + index + 2L,
                    MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + index + 2L).emit);
        }
        assertEquals(MiningDiagnosticSessionBudget.RESERVED_CRITICAL_EVENTS - 1,
                budget.snapshot().criticalEmissions());
        assertEquals(MiningDiagnosticSessionBudget.SESSION_HARD_CAP,
                budget.snapshot().sessionEmissions());
    }
}
