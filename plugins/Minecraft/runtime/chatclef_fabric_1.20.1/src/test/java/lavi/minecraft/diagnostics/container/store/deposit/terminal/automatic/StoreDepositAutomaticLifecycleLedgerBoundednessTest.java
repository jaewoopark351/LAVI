package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBudgetConstants;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.captureOutput;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.occurrences;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep automatic terminal suppression, coverage summary, and LRU assertions with the ledger owner.
class StoreDepositAutomaticLifecycleLedgerBoundednessTest {

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
    @DisplayName("scenario 15 [assertion 22b]: automatic terminal suppression and LRU eviction remain bounded")
    void automaticCoverageSuppressionSummaryRemainsBounded() throws IOException {
        StoreDepositEmissionGate saturatedCriticalGate = saturatedCriticalGate();
        StoreDepositAutomaticLifecycleLedger saturatedLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositAutomaticTerminalDiagnostics saturatedTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(saturatedLedger, saturatedCriticalGate);
        ChatClefDiagnostics.setBoundaryEnabled(true);
        String coverageSuppressionOutput = captureOutput(() -> {
            Task firstSuppressedCoverage = new TestTask("first-suppressed-coverage");
            saturatedTerminals.beginRun(firstSuppressedCoverage, null, 91L);
            saturatedTerminals.recordCoverageClosed(
                    firstSuppressedCoverage,
                    "FIRST_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"
            );
            Task secondSuppressedCoverage = new TestTask("second-suppressed-coverage");
            saturatedTerminals.beginRun(secondSuppressedCoverage, null, 92L);
            saturatedTerminals.recordCoverageClosed(
                    secondSuppressedCoverage,
                    "SECOND_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"
            );
        });
        assertEquals(1, occurrences(
                coverageSuppressionOutput,
                "STORE_DEPOSIT_AUTOMATIC_COVERAGE_SUPPRESSION_SUMMARY"
        ));
        assertTrue(coverageSuppressionOutput.contains("FIRST_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"));
        assertTrue(coverageSuppressionOutput.contains("missingLifecycleBoundaries"));
        assertFalse(coverageSuppressionOutput.contains("SECOND_COVERAGE_CLOSE_AFTER_ALL_SHARED_CAPS"));
        String terminalEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/terminal/StoreDepositAutomaticTerminalDiagnostics.java"
        );
        assertFalse(terminalEmitter.contains("ChatClefDiagnostics.logBoundary("));
        assertTrue(terminalEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(terminalEmitter.contains("STORE_DEPOSIT_AUTOMATIC_COVERAGE_SUPPRESSION_SUMMARY"));
        assertTrue(terminalEmitter.contains("shouldEmitCoverageSuppressionSummary()"));
        assertTrue(terminalEmitter.contains("snapshot.missingLifecycleBoundaries()"));
        assertTrue(coverageSuppressionOutput.contains("diagnosticsMode=BOUNDARY"));
        assertTrue(coverageSuppressionOutput.contains("gameTick="));
        assertTrue(coverageSuppressionOutput.contains("dimension="));
        assertTrue(coverageSuppressionOutput.contains("topLevelTask="));
        assertTrue(coverageSuppressionOutput.contains("activeParentTask="));
        assertTrue(coverageSuppressionOutput.contains("activeChildTask="));
    }

    @Test
    @DisplayName("scenario 15 [assertion 22b]: terminal emission suppression remains explicit")
    void terminalEmissionSuppressionRemainsExplicit() {
        StoreDepositAutomaticLifecycleLedger terminalLedger =
                new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("suppressed-maintenance");
        StoreDepositAutomaticContext terminalContext = terminalLedger.beginRun(
                maintenance,
                null,
                1L
        );
        StoreDepositAutomaticLifecycleLedger.TerminalRecord suppressedTerminal =
                terminalLedger.recordScope(
                        terminalContext,
                        "TRANSFER",
                        "transfer-suppressed",
                        "TASK_STOP_BEGIN"
                );
        StoreDepositAutomaticLifecycleLedger.Snapshot suppressedSnapshot =
                terminalLedger.recordTerminalEmissionSuppressed(suppressedTerminal);
        assertFalse(suppressedSnapshot.terminalIdentityCoverageComplete());
        assertEquals(1L, suppressedSnapshot.terminalEmissionSuppressedCount());
        assertEquals(1, suppressedSnapshot.terminalEmissionSuppressedCounts().get("TRANSFER"));
        assertEquals("TASK_STOP_BEGIN", suppressedSnapshot.terminalEmissionSuppressedReasons().get("TRANSFER"));
    }

    @Test
    @DisplayName("scenario 15 [assertion 22b]: active run access-order LRU and post-removal suppression remain exact")
    void activeRunAccessOrderLruAndPostRemovalSuppressionRemainExact() {
        StoreDepositAutomaticLifecycleLedger evictionLedger =
                new StoreDepositAutomaticLifecycleLedger();
        List<Task> maintenanceRuns = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            Task run = new TestTask("bounded-run-" + index);
            maintenanceRuns.add(run);
            evictionLedger.beginRun(run, null, index);
        }
        assertTrue(evictionLedger.contextForMaintenance(maintenanceRuns.get(0)).available());
        Task seventeenth = new TestTask("bounded-run-16");
        evictionLedger.beginRun(seventeenth, null, 16L);
        StoreDepositAutomaticLifecycleLedger.TerminalRecord eviction =
                evictionLedger.takePendingEviction();
        assertTrue(eviction.available());
        assertEquals("ACTIVE_RUN_LEDGER_CAP_EVICTION", eviction.terminalReason());
        assertFalse(eviction.snapshot().terminalIdentityCoverageComplete());
        assertEquals(1L, eviction.snapshot().activeRunLedgerEvictionCount());
        assertTrue(evictionLedger.contextForMaintenance(maintenanceRuns.get(0)).available());
        assertFalse(evictionLedger.contextForMaintenance(maintenanceRuns.get(1)).available());
        StoreDepositAutomaticLifecycleLedger.Snapshot suppressedEviction =
                evictionLedger.recordTerminalEmissionSuppressed(eviction);
        assertEquals(1L, suppressedEviction.activeRunEvictionEmissionSuppressedCount());
        assertEquals(1L, suppressedEviction.terminalEmissionSuppressedCount());
    }

    private static StoreDepositEmissionGate saturatedCriticalGate() {
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_CONTROL_EVENTS; index++) {
            gate.shouldEmitControl("control-operation-" + index, "control-event-" + index);
        }
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_LATE_SUMMARIES; index++) {
            gate.shouldEmitLateSummary("late-operation-" + index);
        }
        return gate;
    }
}
