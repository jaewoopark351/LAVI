package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Stress cumulative terminal accounting beyond every physical Store quota.
class StoreDepositTerminalLedgerStressTest {
    @Test
    void oneThousandTwentyFiveMixedTerminalsRemainSettledExactAndBounded() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        StoreDepositTerminalClassification[] classifications =
                StoreDepositTerminalClassification.values();
        StoreDepositTerminalScope[] scopes = StoreDepositTerminalScope.values();

        for (int index = 0; index < 1_025; index++) {
            StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                    "stress-operation-" + index,
                    classifications[index % classifications.length],
                    scopes[index % scopes.length],
                    (index & 1) == 0,
                    index
            );
            assertNotNull(token);
            if ((index & 1) == 0) {
                assertTrue(ledger.recordAdmissionGranted(token));
                assertTrue(ledger.recordEmissionCompleted(token));
            } else {
                assertTrue(ledger.recordSuppressedBeforeAdmission(token));
            }
        }

        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(1_025, snapshot.totalTerminalObserved());
        assertEquals(1_025, snapshot.terminalSequence());
        assertEquals(0, snapshot.fullTerminalGroupAdmissionPending());
        assertEquals(513, snapshot.fullTerminalGroupAdmissionGranted());
        assertEquals(512, snapshot.fullTerminalGroupSuppressedBeforeAdmission());
        assertEquals(0, snapshot.fullTerminalGroupEmissionPending());
        assertEquals(513, snapshot.fullTerminalGroupEmissionCompleted());
        assertEquals(257, snapshot.classificationCounts().get(
                StoreDepositTerminalClassification.NATURAL_FINISH));
        assertEquals(256, snapshot.classificationCounts().get(
                StoreDepositTerminalClassification.EXPLICIT_STOP));
        assertEquals(256, snapshot.classificationCounts().get(
                StoreDepositTerminalClassification.UNKNOWN_STOP));
        assertEquals(256, snapshot.classificationCounts().get(
                StoreDepositTerminalClassification.UNAVAILABLE));
        assertEquals(342, snapshot.scopeCounts().get(StoreDepositTerminalScope.MANUAL));
        assertEquals(342, snapshot.scopeCounts().get(
                StoreDepositTerminalScope.AUTOMATIC_GENERAL));
        assertEquals(341, snapshot.scopeCounts().get(StoreDepositTerminalScope.UNAVAILABLE));
        assertEquals(513, snapshot.terminalContextAvailableCount());
        assertEquals(512, snapshot.terminalContextUnavailableCount());
        assertEquals(StoreDepositTerminalLedger.MAX_ABNORMAL_SAMPLES,
                snapshot.recentAbnormalSamples().size());
        assertEquals(760, snapshot.omittedAbnormalSampleCount());
        assertEquals(0, snapshot.activeAccountingTokenCount());
        assertEquals(0, snapshot.activeTokenRegistryOverflowCount());
        assertTrue(snapshot.saturatedCounterFlags().isEmpty());
        assertTrue(snapshot.settled());
        assertTrue(snapshot.accountingEquationsHold());
        assertEquals(StoreDepositTerminalReconciliationStatus.EXACT,
                snapshot.reconciliationStatus());
    }

    @Test
    void activeTokenRegistryRejectsOverflowWithoutEvictionAndRecoversAfterSettlement() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        List<StoreDepositTerminalAccountingToken> tokens = new ArrayList<>();
        for (int index = 0;
             index < StoreDepositTerminalLedger.MAX_ACTIVE_ACCOUNTING_TOKENS;
             index++) {
            StoreDepositTerminalRegistrationResult registration =
                    ledger.registerAuthoritativeTerminal(
                            "unsettled-" + index,
                            StoreDepositTerminalClassification.NATURAL_FINISH,
                            StoreDepositTerminalScope.MANUAL,
                            true,
                            index
                    );
            assertTrue(registration.accepted());
            tokens.add(registration.token());
        }

        StoreDepositTerminalRegistrationResult overflow =
                ledger.registerAuthoritativeTerminal(
                        "must-not-evict-oldest",
                        StoreDepositTerminalClassification.UNKNOWN_STOP,
                        StoreDepositTerminalScope.UNAVAILABLE,
                        false,
                        100L
                );
        assertFalse(overflow.accepted());
        assertEquals(StoreDepositTerminalRegistrationResult.Status.ACTIVE_TOKEN_REGISTRY_FULL,
                overflow.status());

        StoreDepositTerminalLedgerSnapshot overflowSnapshot = ledger.snapshot();
        assertEquals(StoreDepositTerminalLedger.MAX_ACTIVE_ACCOUNTING_TOKENS,
                overflowSnapshot.totalTerminalObserved());
        assertEquals(StoreDepositTerminalLedger.MAX_ACTIVE_ACCOUNTING_TOKENS,
                overflowSnapshot.terminalSequence());
        assertEquals(StoreDepositTerminalLedger.MAX_ACTIVE_ACCOUNTING_TOKENS,
                overflowSnapshot.fullTerminalGroupAdmissionPending());
        assertEquals(StoreDepositTerminalLedger.MAX_ACTIVE_ACCOUNTING_TOKENS,
                overflowSnapshot.activeAccountingTokenCount());
        assertEquals(1, overflowSnapshot.activeTokenRegistryOverflowCount());
        assertEquals(1, overflowSnapshot.ledgerCoverageGapCount());
        assertTrue(ledger.recordSuppressedBeforeAdmission(tokens.get(0)));
        for (int index = 1; index < tokens.size(); index++) {
            assertTrue(ledger.recordSuppressedBeforeAdmission(tokens.get(index)));
        }

        StoreDepositTerminalRegistrationResult recovered =
                ledger.registerAuthoritativeTerminal(
                        "accepted-after-capacity-returned",
                        StoreDepositTerminalClassification.NATURAL_FINISH,
                        StoreDepositTerminalScope.MANUAL,
                        true,
                        101L
                );
        assertTrue(recovered.accepted());
        assertTrue(ledger.recordSuppressedBeforeAdmission(recovered.token()));

        StoreDepositTerminalLedgerSnapshot settled = ledger.snapshot();
        assertEquals(0, settled.activeAccountingTokenCount());
        assertEquals(0, settled.fullTerminalGroupAdmissionPending());
        assertEquals(StoreDepositTerminalLedger.MAX_ACTIVE_ACCOUNTING_TOKENS + 1L,
                settled.fullTerminalGroupSuppressedBeforeAdmission());
        assertTrue(settled.settled());
        assertTrue(settled.accountingEquationsHold());
        assertEquals(StoreDepositTerminalReconciliationStatus.COVERAGE_GAP,
                settled.reconciliationStatus());
    }

    @Test
    void firstAndLastSuppressedIdentityAndTickRemainBoundedAndOrdered() {
        StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
        String firstIdentity = "first-" + "a".repeat(300);
        String lastIdentity = "last-" + "z".repeat(300);

        StoreDepositTerminalAccountingToken first = ledger.recordAuthoritativeTerminal(
                firstIdentity,
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                11L
        );
        assertTrue(ledger.recordSuppressedBeforeAdmission(first));
        StoreDepositTerminalAccountingToken middle = ledger.recordAuthoritativeTerminal(
                "middle",
                StoreDepositTerminalClassification.NATURAL_FINISH,
                StoreDepositTerminalScope.MANUAL,
                true,
                50L
        );
        assertTrue(ledger.recordAdmissionGranted(middle));
        assertTrue(ledger.recordEmissionCompleted(middle));
        StoreDepositTerminalAccountingToken last = ledger.recordAuthoritativeTerminal(
                lastIdentity,
                StoreDepositTerminalClassification.UNKNOWN_STOP,
                StoreDepositTerminalScope.UNAVAILABLE,
                false,
                99L
        );
        assertTrue(ledger.recordSuppressedBeforeAdmission(last));

        StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(firstIdentity.substring(0, 160), snapshot.firstSuppressedOperationId());
        assertEquals(11L, snapshot.firstSuppressedTick());
        assertEquals(lastIdentity.substring(0, 160), snapshot.lastSuppressedOperationId());
        assertEquals(99L, snapshot.lastSuppressedTick());
        assertEquals(2, snapshot.fullTerminalGroupSuppressedBeforeAdmission());
    }
}
