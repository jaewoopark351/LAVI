package lavi.minecraft.diagnostics.mining.budget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Prove 256/correlation and 5000/session limits retain a 32-event critical reserve.
class MiningDiagnosticSessionBudgetTest {
    @Test
    void sessionDetailLimitLeavesThirtyTwoCriticalSlotsInsideTheHardCap() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();

        for (int index = 0; index < MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT; index++) {
            String correlation = "correlation-" + (index / MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
            assertTrue(budget.admitDetail(correlation).admitted());
        }

        MiningDiagnosticAdmission rejected = budget.admitDetail("overflow-correlation");
        assertFalse(rejected.admitted());
        assertTrue(rejected.reportSessionCap());
        assertEquals(1, rejected.snapshot().capSignalEmissions());
        assertEquals(MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT + 1,
                rejected.snapshot().sessionEmissions());

        for (int index = 0; index < MiningDiagnosticSessionBudget.RESERVED_CRITICAL_EVENTS - 1; index++) {
            assertTrue(budget.admitCritical("terminal-" + index).admitted());
        }
        assertEquals(MiningDiagnosticSessionBudget.SESSION_HARD_CAP, budget.snapshot().sessionEmissions());
        assertEquals(MiningDiagnosticSessionBudget.RESERVED_CRITICAL_EVENTS - 1,
                budget.snapshot().criticalEmissions());
    }

    @Test
    void sessionCapSignalIsClaimedExactlyOnceAndNotByCorrelationCap() {
        MiningDiagnosticSessionBudget correlationBudget = new MiningDiagnosticSessionBudget();
        for (int index = 0; index < MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT; index++) {
            assertTrue(correlationBudget.admitDetail("one-operation").admitted());
        }
        MiningDiagnosticAdmission operationRejected =
                correlationBudget.admitDetail("one-operation");
        assertFalse(operationRejected.admitted());
        assertFalse(operationRejected.reportSessionCap());
        assertEquals(0, correlationBudget.snapshot().capSignalEmissions());

        MiningDiagnosticSessionBudget sessionBudget = new MiningDiagnosticSessionBudget();
        for (int index = 0; index < MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT; index++) {
            String correlation = "correlation-" + (index / MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
            assertTrue(sessionBudget.admitDetail(correlation).admitted());
        }
        assertTrue(sessionBudget.admitDetail("overflow-a").reportSessionCap());
        for (int index = 0; index < 1_000; index++) {
            assertFalse(sessionBudget.admitDetail("overflow-" + index).reportSessionCap());
        }
        assertEquals(1, sessionBudget.snapshot().capSignalEmissions());
    }

    @Test
    void sessionCapWinsWhenCorrelationAndSessionAreBothSaturated() {
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        for (int index = 0; index < MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT; index++) {
            assertTrue(budget.admitDetail("saturated-correlation").admitted());
        }
        for (int index = MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT;
             index < MiningDiagnosticSessionBudget.SESSION_DETAIL_LIMIT;
             index++) {
            String correlation = "filler-" + (index / MiningDiagnosticSessionBudget.CORRELATION_DETAIL_LIMIT);
            assertTrue(budget.admitDetail(correlation).admitted());
        }

        MiningDiagnosticAdmission rejected = budget.admitDetail("saturated-correlation");
        assertFalse(rejected.admitted());
        assertEquals("SESSION_DETAIL_CAP", rejected.reason());
        assertTrue(rejected.reportSessionCap());
        assertEquals(1, rejected.snapshot().capSignalEmissions());
    }

    @Test
    void explicitResetRestoresAllSessionCapacity() {
        assertTrue(new MiningDiagnosticSessionBudget().canAdmitCritical());
        MiningDiagnosticSessionBudget budget = new MiningDiagnosticSessionBudget();
        assertTrue(budget.admitDetail("correlation").admitted());
        assertEquals(1, budget.snapshot().sessionEmissions());

        budget.reset();

        assertEquals(0, budget.snapshot().sessionEmissions());
        assertEquals(0, budget.snapshot().capSignalEmissions());
        assertFalse(budget.snapshot().sessionCapSignalClaimed());
        assertTrue(budget.canAdmitDetail("correlation"));
    }
}
