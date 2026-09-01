package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Link immutable cumulative snapshots to terminal coverage and prior emission calls.
class StoreDepositTerminalSnapshotProvenanceTest {
    @Test
    void captureSequenceRevisionAndCoveredTerminalSequenceAreMonotonic() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        StoreDepositTerminalAccountingToken first = ledger.recordAuthoritativeTerminal(
                "first-terminal",
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                1L
        );
        assertTrue(ledger.recordSuppressedBeforeAdmission(first));

        StoreDepositTerminalLedgerSnapshot observational = ledger.snapshot();
        StoreDepositTerminalLedgerSnapshot repeatedObservation = ledger.snapshot();
        assertEquals(observational, repeatedObservation);
        assertEquals(0, observational.snapshotSequence());
        assertEquals(2, observational.ledgerRevision());

        StoreDepositTerminalLedgerSnapshot firstSnapshot = ledger.captureSnapshot();
        assertEquals(1, firstSnapshot.snapshotSequence());
        assertTrue(firstSnapshot.snapshotSequenceAvailable());
        assertEquals(0, firstSnapshot.previousCreatedSnapshotSequence());
        assertEquals(0, firstSnapshot.previousEmissionCallsReturnedSnapshotSequence());
        assertEquals(1, firstSnapshot.coveredThroughTerminalSequence());
        assertEquals("CUMULATIVE", firstSnapshot.accountingMode());
        assertEquals("ADMISSION_INCLUDED_EMISSION_OUTCOME_EXCLUDED",
                firstSnapshot.snapshotSelfAccounting());
        assertEquals(3, firstSnapshot.ledgerRevision());
        assertFalse(ledger.recordSnapshotEmissionCallsReturned(0L));
        assertFalse(ledger.recordSnapshotEmissionCallsReturned(2L));
        assertEquals(firstSnapshot.ledgerRevision(), ledger.snapshot().ledgerRevision());
        assertTrue(ledger.recordSnapshotEmissionCallsReturned(1L));
        assertFalse(ledger.recordSnapshotEmissionCallsReturned(1L));

        StoreDepositTerminalAccountingToken second = ledger.recordAuthoritativeTerminal(
                "second-terminal",
                StoreDepositTerminalClassification.UNKNOWN_STOP,
                StoreDepositTerminalScope.AUTOMATIC_GENERAL,
                false,
                2L
        );
        StoreDepositTerminalLedgerSnapshot secondSnapshot = ledger.captureSnapshot();
        assertEquals(2, secondSnapshot.snapshotSequence());
        assertEquals(1, secondSnapshot.previousCreatedSnapshotSequence());
        assertEquals(1, secondSnapshot.previousEmissionCallsReturnedSnapshotSequence());
        assertEquals(2, secondSnapshot.coveredThroughTerminalSequence());
        assertEquals(1, secondSnapshot.fullTerminalGroupAdmissionPending());
        assertTrue(secondSnapshot.ledgerRevision() > firstSnapshot.ledgerRevision());
        assertTrue(ledger.recordSuppressedBeforeAdmission(second));

        StoreDepositTerminalLedgerSnapshot thirdSnapshot = ledger.captureSnapshot();
        assertEquals(3, thirdSnapshot.snapshotSequence());
        assertEquals(2, thirdSnapshot.previousCreatedSnapshotSequence());
        assertEquals(1, thirdSnapshot.previousEmissionCallsReturnedSnapshotSequence());
        assertEquals(2, thirdSnapshot.coveredThroughTerminalSequence());
        assertEquals(0, thirdSnapshot.fullTerminalGroupAdmissionPending());
        assertTrue(thirdSnapshot.ledgerRevision() > secondSnapshot.ledgerRevision());

        StoreDepositTerminalLedgerSnapshot pureAfterCapture = ledger.snapshot();
        assertEquals(0L, pureAfterCapture.snapshotSequence());
        assertEquals(3L, pureAfterCapture.previousCreatedSnapshotSequence());
        long revisionBeforeRepeat = pureAfterCapture.ledgerRevision();
        for (int index = 0; index < 100; index++) {
            assertEquals(pureAfterCapture, ledger.snapshot());
        }
        assertEquals(revisionBeforeRepeat, ledger.snapshot().ledgerRevision());
    }

    @Test
    void saturatedSnapshotSequenceIsUsedOnceAndNeverReused() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger(
                0L,
                0L,
                Long.MAX_VALUE - 1L
        );

        StoreDepositTerminalLedgerSnapshot last = ledger.captureSnapshot();
        StoreDepositTerminalLedgerSnapshot unavailable = ledger.captureSnapshot();

        assertEquals(Long.MAX_VALUE, last.snapshotSequence());
        assertFalse(last.snapshotSequenceAvailable());
        assertEquals(Long.MAX_VALUE - 1L, last.previousCreatedSnapshotSequence());
        assertTrue(last.saturatedCounterFlags().contains(
                StoreDepositTerminalSaturationFlag.SNAPSHOT_SEQUENCE));
        assertEquals(StoreDepositTerminalReconciliationStatus.SATURATED,
                last.reconciliationStatus());
        assertEquals(0L, unavailable.snapshotSequence());
        assertFalse(unavailable.snapshotSequenceAvailable());
        assertEquals(Long.MAX_VALUE,
                unavailable.previousCreatedSnapshotSequence());
        assertEquals(last.coveredThroughTerminalSequence(),
                unavailable.coveredThroughTerminalSequence());
        assertEquals(last.ledgerRevision() + 1L, unavailable.ledgerRevision());
        assertEquals(1L, unavailable.ledgerCoverageGapCount());
    }
}
