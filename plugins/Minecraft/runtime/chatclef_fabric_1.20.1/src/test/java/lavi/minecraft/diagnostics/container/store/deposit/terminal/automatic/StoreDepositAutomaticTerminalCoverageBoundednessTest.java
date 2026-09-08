package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Characterize terminal identity, scope, gap, and suppression capacity boundaries.
class StoreDepositAutomaticTerminalCoverageBoundednessTest {
    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void observedTerminalDedupeRetainsAccessOrderAtTheExactCap() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("observed-dedupe-cap");
        StoreDepositAutomaticContext context = ledger.beginRun(maintenance, null, 1L);
        for (int index = 0; index < 1_024; index++) {
            ledger.recordScope(context, "TRANSFER", "transfer-" + index, "CLOSED");
        }

        assertTrue(ledger.recordScope(
                context,
                "TRANSFER",
                "transfer-0",
                "DUPLICATE_TOUCH"
        ).duplicate());
        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = ledger.recordScope(
                context,
                "TRANSFER",
                "transfer-1024",
                "CAP_EVICTION"
        ).snapshot();

        assertEquals(1_024, snapshot.observedTerminalIdentities().size());
        assertTrue(snapshot.observedTerminalIdentities().contains("TRANSFER|transfer-0"));
        assertFalse(snapshot.observedTerminalIdentities().contains("TRANSFER|transfer-1"));
        assertTrue(snapshot.observedTerminalIdentities().contains("TRANSFER|transfer-1024"));
        assertEquals(1_025, snapshot.terminalCount("TRANSFER"));
        assertEquals(1L, snapshot.terminalDedupeEvictionCount());
        assertFalse(snapshot.terminalIdentityCoverageComplete());
    }

    @Test
    void expectedTerminalIdentityOverflowDoesNotEvictRetainedIdentities() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("expected-identity-cap");
        StoreDepositAutomaticContext context = ledger.beginRun(maintenance, null, 2L);
        for (int index = 0; index < 1_024; index++) {
            ledger.expectTerminalScopeIdentity(context, "TRANSFER", "transfer-" + index);
        }
        ledger.expectTerminalScopeIdentity(context, "TRANSFER", "transfer-0");
        ledger.expectTerminalScopeIdentity(context, "TRANSFER", "transfer-1024");

        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = ledger.snapshotFor(maintenance);
        assertEquals(1_024, snapshot.expectedTerminalCount("TRANSFER"));
        assertEquals(1_024, snapshot.expectedTerminalIdentities().size());
        assertTrue(snapshot.expectedTerminalIdentities().contains("TRANSFER|transfer-0"));
        assertTrue(snapshot.expectedTerminalIdentities().contains("TRANSFER|transfer-1"));
        assertFalse(snapshot.expectedTerminalIdentities().contains("TRANSFER|transfer-1024"));
        assertEquals(1L, snapshot.expectedTerminalIdentityOverflowCount());
        assertFalse(snapshot.terminalIdentityCoverageComplete());
    }

    @Test
    void terminalScopeAndSuppressionMapsUseTheSameExactScopeCap() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("terminal-scope-cap");
        StoreDepositAutomaticContext context = ledger.beginRun(maintenance, null, 3L);
        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = null;
        for (int index = 0; index < 33; index++) {
            StoreDepositAutomaticLifecycleLedger.TerminalRecord record = ledger.recordScope(
                    context,
                    "SCOPE_" + index,
                    "identity-" + index,
                    "SUPPRESSED_" + index
            );
            snapshot = ledger.recordTerminalEmissionSuppressed(record);
        }

        assertEquals(1L, snapshot.terminalScopeOverflowCount());
        assertEquals(0, snapshot.terminalCount("SCOPE_32"));
        assertEquals(1, snapshot.terminalCount("OTHER_SCOPE_OVERFLOW"));
        assertEquals(33L, snapshot.terminalEmissionSuppressedCount());
        assertEquals(1, snapshot.terminalEmissionSuppressedCounts().get("OTHER_SCOPE_OVERFLOW"));
        assertEquals(
                "SUPPRESSED_32",
                snapshot.terminalEmissionSuppressedReasons().get("OTHER_SCOPE_OVERFLOW")
        );
        assertFalse(snapshot.terminalIdentityCoverageComplete());
    }

    @Test
    void diagnosticCoverageGapOverflowRetainsTheFirstExactEntries() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("coverage-gap-cap");
        StoreDepositAutomaticContext context = ledger.beginRun(maintenance, null, 4L);
        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = null;
        for (int index = 0; index < 257; index++) {
            snapshot = ledger.recordDiagnosticCoverageGap(
                    context,
                    "GAP_SCOPE",
                    "identity-" + index,
                    "reason-" + index
            );
        }

        assertEquals(256, snapshot.diagnosticCoverageGaps().size());
        assertTrue(snapshot.diagnosticCoverageGaps().containsKey("GAP_SCOPE|identity-0"));
        assertTrue(snapshot.diagnosticCoverageGaps().containsKey("GAP_SCOPE|identity-255"));
        assertFalse(snapshot.diagnosticCoverageGaps().containsKey("GAP_SCOPE|identity-256"));
        assertEquals(1L, snapshot.diagnosticCoverageGapOverflowCount());
        assertFalse(snapshot.terminalIdentityCoverageComplete());
    }

    @Test
    void nullAndBlankTerminalValuesNormalizeToUnavailable() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("normalization");
        StoreDepositAutomaticContext context = ledger.beginRun(maintenance, null, 5L);
        ledger.expectTerminalScopeIdentity(context, null, " ");
        ledger.recordDiagnosticCoverageGap(context, null, "", null);
        StoreDepositAutomaticLifecycleLedger.TerminalRecord record = ledger.recordScope(
                context,
                null,
                " ",
                null
        );

        assertEquals("UNAVAILABLE", record.terminalScope());
        assertEquals("UNAVAILABLE", record.closingIdentity());
        assertEquals("UNAVAILABLE", record.terminalReason());
        assertTrue(record.snapshot().observedTerminalIdentities().contains("UNAVAILABLE|UNAVAILABLE"));
        assertEquals(
                "EXPECTED_TERMINAL_IDENTITY_UNAVAILABLE",
                record.snapshot().diagnosticCoverageGaps()
                        .get("EXPECTED_TERMINAL_IDENTITY|UNAVAILABLE|UNAVAILABLE")
        );
        assertEquals(
                "UNAVAILABLE",
                record.snapshot().diagnosticCoverageGaps().get("UNAVAILABLE|UNAVAILABLE")
        );
        assertFalse(record.snapshot().terminalIdentityCoverageComplete());
    }
}
