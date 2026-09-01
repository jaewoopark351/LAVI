package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Verify bounded Store terminal accounting independently of physical log retention.
class StoreDepositTerminalLedgerTest {
    @Test
    void suppressedRoutineGroupsStillCloseCumulativeAccountingBeforeAnAbnormalTerminal() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();

        for (int index = 0; index < 8; index++) {
            StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                    "routine-" + index,
                    StoreDepositTerminalClassification.NATURAL_FINISH,
                    StoreDepositTerminalScope.AUTOMATIC_GENERAL,
                    true,
                    index
            );
            assertNotNull(token);
            if (index < 2) {
                assertTrue(ledger.recordAdmissionGranted(token));
                assertTrue(ledger.recordEmissionCompleted(token));
            } else {
                assertTrue(ledger.recordSuppressedBeforeAdmission(token));
            }
        }

        StoreDepositTerminalAccountingToken abnormal = ledger.recordAuthoritativeTerminal(
                "abnormal-8",
                StoreDepositTerminalClassification.EXPLICIT_STOP,
                StoreDepositTerminalScope.AUTOMATIC_GENERAL,
                true,
                8
        );
        assertTrue(ledger.recordAdmissionGranted(abnormal));
        assertTrue(ledger.recordEmissionCompleted(abnormal));

        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(9, snapshot.totalTerminalObserved());
        assertEquals(3, snapshot.fullTerminalGroupAdmissionGranted());
        assertEquals(6, snapshot.fullTerminalGroupSuppressedBeforeAdmission());
        assertEquals(3, snapshot.fullTerminalGroupEmissionCompleted());
        assertEquals(1, snapshot.classificationCounts().get(
                StoreDepositTerminalClassification.EXPLICIT_STOP));
        assertEquals(StoreDepositTerminalReconciliationStatus.EXACT, snapshot.reconciliationStatus());
        assertTrue(snapshot.settled());
        assertTrue(snapshot.accountingEquationsHold());
    }

    @Test
    void emissionFailureConsumesTheGrantedGroupWithoutRefundOrSecondSettlement() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                "failure",
                StoreDepositTerminalClassification.UNKNOWN_STOP,
                StoreDepositTerminalScope.MANUAL,
                false,
                40
        );

        assertTrue(ledger.recordAdmissionGranted(token));
        assertTrue(ledger.recordEmissionFailedAfterAdmission(token));
        assertFalse(ledger.recordEmissionCompleted(token));
        assertFalse(ledger.recordAdmissionGranted(token));

        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(1, snapshot.fullTerminalGroupAdmissionGranted());
        assertEquals(1, snapshot.fullTerminalGroupEmissionFailedAfterAdmission());
        assertEquals(0, snapshot.fullTerminalGroupEmissionCompleted());
        assertEquals(1, snapshot.ledgerCoverageGapCount());
        assertEquals(StoreDepositTerminalReconciliationStatus.COVERAGE_GAP,
                snapshot.reconciliationStatus());
        assertTrue(snapshot.settled());
        assertTrue(snapshot.accountingEquationsHold());
    }

    @Test
    void aTokenCannotSettleAnotherLedgersCounters() {
        StoreDepositTerminalLedger owner = new StoreDepositTerminalLedger();
        StoreDepositTerminalLedger foreign = new StoreDepositTerminalLedger();
        StoreDepositTerminalAccountingToken token = owner.recordAuthoritativeTerminal(
                "owner-terminal",
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                1
        );

        assertFalse(foreign.recordAdmissionGranted(token));
        assertTrue(owner.recordSuppressedBeforeAdmission(token));
        assertEquals(0, foreign.snapshot().totalTerminalObserved());
        assertEquals(0, foreign.snapshot().fullTerminalGroupAdmissionGranted());
        assertEquals(1, owner.snapshot().fullTerminalGroupSuppressedBeforeAdmission());
    }

    @Test
    void duplicateAndContextlessBoundariesDoNotFabricateUniqueTerminalTotals() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                "only-terminal",
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                1
        );
        ledger.recordSuppressedBeforeAdmission(token);
        ledger.recordDuplicateFinalizationAttempt();
        ledger.recordTerminalBoundaryWithoutStableContext();

        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(1, snapshot.totalTerminalObserved());
        assertEquals(1, snapshot.duplicateFinalizationAttempts());
        assertEquals(1, snapshot.terminalBoundaryWithoutContextCount());
        assertEquals(StoreDepositTerminalReconciliationStatus.COVERAGE_GAP,
                snapshot.reconciliationStatus());
        assertTrue(snapshot.accountingEquationsHold());
    }

    @Test
    void abnormalSamplesRemainBoundedWhileAggregateCountsKeepGrowing() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        for (int index = 0; index < 100; index++) {
            StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                    "abnormal-operation-with-a-long-identity-" + index + "-" + "x".repeat(200),
                    StoreDepositTerminalClassification.UNKNOWN_STOP,
                    StoreDepositTerminalScope.UNAVAILABLE,
                    false,
                    index
            );
            ledger.recordSuppressedBeforeAdmission(token);
        }

        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(100, snapshot.totalTerminalObserved());
        assertEquals(StoreDepositTerminalLedger.MAX_ABNORMAL_SAMPLES,
                snapshot.recentAbnormalSamples().size());
        assertEquals(92, snapshot.omittedAbnormalSampleCount());
        assertTrue(snapshot.recentAbnormalSamples().stream()
                .allMatch(sample -> sample.operationId().length() <= 160));
        assertEquals(StoreDepositTerminalReconciliationStatus.EXACT, snapshot.reconciliationStatus());
    }

    @Test
    void saturatedSequenceIsNotReusedAsAnotherTerminalIdentity() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger(Long.MAX_VALUE - 1, 0);

        StoreDepositTerminalAccountingToken last = ledger.recordAuthoritativeTerminal(
                "last",
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                1
        );
        StoreDepositTerminalRegistrationResult unavailable = ledger.registerAuthoritativeTerminal(
                "must-not-reuse-max",
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                2
        );

        assertNotNull(last);
        assertEquals(Long.MAX_VALUE, last.terminalSequence());
        assertFalse(unavailable.accepted());
        assertNull(unavailable.token());
        assertEquals(StoreDepositTerminalRegistrationResult.Status.SEQUENCE_UNAVAILABLE,
                unavailable.status());
        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(1, snapshot.totalTerminalObserved());
        assertFalse(snapshot.sequenceAvailable());
        assertEquals(StoreDepositSequenceUnavailableReason.UNAVAILABLE_SATURATED,
                snapshot.sequenceUnavailableReason());
        assertTrue(snapshot.saturatedCounterFlags().contains(
                StoreDepositTerminalSaturationFlag.TERMINAL_SEQUENCE));
        assertEquals(StoreDepositTerminalReconciliationStatus.SATURATED,
                snapshot.reconciliationStatus());
    }
}
