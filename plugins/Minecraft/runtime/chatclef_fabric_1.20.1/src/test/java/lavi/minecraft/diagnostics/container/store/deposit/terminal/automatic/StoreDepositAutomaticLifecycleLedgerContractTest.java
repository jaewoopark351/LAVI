package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Split the automatic-ledger contract from transfer-registry coverage integration.
class StoreDepositAutomaticLifecycleLedgerContractTest {

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
    @DisplayName("scenario 14 [assertion 21]: lifecycle terminal scopes have separate exact counts")
    void automaticLifecycleScopesKeepSeparateExactCountsAndHandoffFlags() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("maintenance");
        Task userRoot = new TestTask("user-root");
        Task itemRootOne = new TestTask("item-root-1");
        Task itemRootTwo = new TestTask("item-root-2");
        ledger.beginRun(maintenance, userRoot, 77L);
        StoreDepositAutomaticContext firstChild = ledger.registerChild(maintenance, itemRootOne, 0);
        StoreDepositAutomaticContext secondChild = ledger.registerChild(maintenance, itemRootTwo, 1);

        ledger.expectTerminalScopeIdentity(firstChild, "SLOT_MUTATION", "mutation-1");
        ledger.expectTerminalScopeIdentity(firstChild, "TRANSFER", "transfer-1");
        ledger.expectTerminalScopeIdentity(firstChild, "CANDIDATE_INVALIDATION", "candidate-1");
        ledger.expectTerminalScopeIdentity(firstChild, "ROUTE_RECONCILIATION", "reconcile-1");
        ledger.expectTerminalScopeIdentity(firstChild, "ROUTE_CHILD", "route-1");
        ledger.recordScope(firstChild, "SLOT_MUTATION", "mutation-1", "LOCAL_MUTATION");
        ledger.recordScope(firstChild, "TRANSFER", "transfer-1", "RECONCILED");
        ledger.recordScope(firstChild, "CANDIDATE_INVALIDATION", "candidate-1", "CHECK_FALSE");
        ledger.recordScope(firstChild, "ROUTE_RECONCILIATION", "reconcile-1", "REPLACED");
        ledger.recordScope(firstChild, "ROUTE_CHILD", "route-1", "STOPPED");
        ledger.recordScope(
                firstChild,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(itemRootOne),
                "NATURAL_FINISH"
        );
        ledger.recordScope(
                secondChild,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(itemRootTwo),
                "NATURAL_FINISH"
        );
        StoreDepositAutomaticLifecycleLedger.TerminalRecord duplicate = ledger.recordScope(
                firstChild, "TRANSFER", "transfer-1", "DUPLICATE"
        );
        ledger.recordMaintenanceTerminal(maintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(maintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(maintenance, "REARM", "WAIT_FOR_REARM");
        ledger.observeUserTaskResume(userRoot);
        ledger.observeUserTaskNaturalCompletion(userRoot);
        ledger.recordCoverageCloseForUserTask(userRoot, "USER_TASK_TERMINAL");

        StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = ledger.snapshotFor(maintenance);
        assertTrue(duplicate.duplicate());
        assertEquals(firstChild.autoOperationId(), secondChild.autoOperationId());
        assertNotEquals(firstChild.autoChildOperationId(), secondChild.autoChildOperationId());
        assertEquals(1, snapshot.terminalCount("SLOT_MUTATION"));
        assertEquals(1, snapshot.terminalCount("TRANSFER"));
        assertEquals(1, snapshot.terminalCount("CANDIDATE_INVALIDATION"));
        assertEquals(1, snapshot.terminalCount("ROUTE_RECONCILIATION"));
        assertEquals(1, snapshot.terminalCount("ROUTE_CHILD"));
        assertEquals(1, snapshot.expectedTerminalCount("SLOT_MUTATION"));
        assertEquals(1, snapshot.expectedTerminalCount("TRANSFER"));
        assertEquals(1, snapshot.expectedTerminalCount("CANDIDATE_INVALIDATION"));
        assertEquals(1, snapshot.expectedTerminalCount("ROUTE_RECONCILIATION"));
        assertEquals(1, snapshot.expectedTerminalCount("ROUTE_CHILD"));
        assertEquals(2, snapshot.terminalCount("PER_ITEM_ROOT"));
        assertEquals(1, snapshot.terminalCount("MAINTENANCE_LOGICAL_TERMINAL"));
        assertEquals(1, snapshot.terminalCount("PRESSURE_OWNED_RUN"));
        assertEquals(1, snapshot.terminalCount("RUNNING_TO_WAIT_FOR_REARM"));
        assertEquals(1, snapshot.terminalCount("USER_TASK_RESUME"));
        assertEquals(1, snapshot.terminalCount("USER_TASK_NATURAL_COMPLETION"));
        assertEquals(1, snapshot.terminalCount("AUTOMATIC_DIAGNOSTIC_COVERAGE"));
        assertTrue(snapshot.maintenanceLogicalTerminal());
        assertTrue(snapshot.pressureOwnedRunClosed());
        assertTrue(snapshot.userTaskResumeObserved());
        assertTrue(snapshot.userTaskNaturalCompletionObserved());
        assertTrue(snapshot.diagnosticCoverageClosed());
        assertTrue(snapshot.lifecycleCoverageComplete());
        assertEquals("NONE", snapshot.missingLifecycleBoundaries());
        assertEquals("WAIT_FOR_REARM", snapshot.nextLifecycleState());
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: same user root retains ordered automatic runs")
    void sameUserRootRetainsOrderedAutomaticRuns() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task userRoot = new TestTask("user-root");
        Task firstMaintenance = new TestTask("maintenance-1");
        StoreDepositAutomaticContext firstRun = ledger.beginRun(firstMaintenance, userRoot, 77L);
        ledger.recordPressureOwnedRunClose(firstMaintenance, "OWNED_RUN_DONE");
        ledger.observeUserTaskResume(userRoot);
        ledger.observeUserTaskNaturalCompletion(userRoot);
        ledger.recordCoverageCloseForUserTask(userRoot, "USER_TASK_TERMINAL");
        Task secondMaintenance = new TestTask("maintenance-2");
        StoreDepositAutomaticContext secondRun = ledger.beginRun(secondMaintenance, userRoot, 78L);
        ledger.recordPressureOwnedRunClose(secondMaintenance, "OWNED_RUN_DONE");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord secondResume =
                ledger.observeUserTaskResume(userRoot);
        List<StoreDepositAutomaticLifecycleLedger.TerminalRecord> naturalCompletions =
                ledger.observeUserTaskNaturalCompletions(userRoot);
        List<StoreDepositAutomaticLifecycleLedger.TerminalRecord> coverageCloses =
                ledger.recordCoverageClosesForUserTask(userRoot, "USER_TASK_TERMINAL");
        assertNotEquals(firstRun.autoOperationId(), secondRun.autoOperationId());
        assertEquals(secondRun.autoOperationId(), secondResume.context().autoOperationId());
        assertEquals(1, naturalCompletions.size());
        assertEquals(secondRun.autoOperationId(), naturalCompletions.get(0).context().autoOperationId());
        assertEquals(1, coverageCloses.size());
        assertEquals(secondRun.autoOperationId(), coverageCloses.get(0).context().autoOperationId());
        assertFalse(coverageCloses.get(0).snapshot().lifecycleCoverageComplete());
        assertTrue(coverageCloses.get(0).snapshot().missingLifecycleBoundaries()
                .contains("MAINTENANCE_LOGICAL_TERMINAL"));
        assertTrue(coverageCloses.get(0).snapshot().missingLifecycleBoundaries()
                .contains("RUNNING_TO_WAIT_FOR_REARM"));
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: missing per-item close remains incomplete")
    void missingPerItemCloseRemainsIncomplete() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task partialMaintenance = new TestTask("maintenance-partial-child-close");
        Task partialUserRoot = new TestTask("user-root-partial-child-close");
        Task partialChildOne = new TestTask("partial-child-1");
        Task partialChildTwo = new TestTask("partial-child-2");
        StoreDepositAutomaticContext partialContext = ledger.beginRun(
                partialMaintenance,
                partialUserRoot,
                79L
        );
        ledger.registerChild(partialMaintenance, partialChildOne, 0);
        ledger.registerChild(partialMaintenance, partialChildTwo, 1);
        ledger.recordScope(
                partialContext,
                "PER_ITEM_ROOT",
                StoreDepositOperationContext.identity(partialChildOne),
                "NATURAL_FINISH"
        );
        ledger.recordMaintenanceTerminal(partialMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(partialMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(partialMaintenance, "REARM", "WAIT_FOR_REARM");
        ledger.observeUserTaskResume(partialUserRoot);
        ledger.observeUserTaskNaturalCompletion(partialUserRoot);
        StoreDepositAutomaticLifecycleLedger.TerminalRecord partialCoverage =
                ledger.recordCoverageCloseForUserTask(
                        partialUserRoot,
                        "USER_TASK_TERMINAL"
                );
        assertEquals(2, partialCoverage.snapshot().registeredChildCount());
        assertEquals(1, partialCoverage.snapshot().terminalCount("PER_ITEM_ROOT"));
        assertFalse(partialCoverage.snapshot().lifecycleCoverageComplete());
        assertTrue(partialCoverage.snapshot().missingLifecycleBoundaries()
                .contains("PER_ITEM_ROOT_TERMINALS(expected=2,observed=1)"));
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: wrong per-item identities remain incomplete")
    void wrongPerItemIdentitiesRemainIncomplete() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task wrongChildMaintenance = new TestTask("maintenance-wrong-child-identities");
        Task expectedChildOne = new TestTask("expected-child-1");
        Task expectedChildTwo = new TestTask("expected-child-2");
        StoreDepositAutomaticContext wrongChildContext = ledger.beginRun(
                wrongChildMaintenance,
                null,
                80L
        );
        ledger.registerChild(wrongChildMaintenance, expectedChildOne, 0);
        ledger.registerChild(wrongChildMaintenance, expectedChildTwo, 1);
        ledger.recordScope(wrongChildContext, "PER_ITEM_ROOT", "unrelated-child-x", "NATURAL_FINISH");
        ledger.recordScope(wrongChildContext, "PER_ITEM_ROOT", "unrelated-child-y", "NATURAL_FINISH");
        ledger.recordMaintenanceTerminal(wrongChildMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(wrongChildMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(wrongChildMaintenance, "REARM", "WAIT_FOR_REARM");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord wrongChildCoverage =
                ledger.recordCoverageClose(
                        wrongChildMaintenance,
                        "NO_USER_TASK_COVERAGE_CLOSE"
                );
        assertEquals(2, wrongChildCoverage.snapshot().terminalCount("PER_ITEM_ROOT"));
        assertFalse(wrongChildCoverage.snapshot().terminalIdentityCoverageComplete());
        assertTrue(wrongChildCoverage.snapshot().missingLifecycleBoundaries()
                .contains("MISSING_PER_ITEM_ROOT_TERMINAL_IDENTITIES"));
        assertTrue(wrongChildCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNEXPECTED_PER_ITEM_ROOT_TERMINAL_IDENTITIES"));
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: missing and unexpected scope identities remain explicit")
    void missingAndUnexpectedScopeIdentitiesRemainExplicit() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task missingTransferMaintenance = new TestTask("maintenance-missing-transfer-close");
        StoreDepositAutomaticContext missingTransferContext = ledger.beginRun(
                missingTransferMaintenance,
                null,
                81L
        );
        ledger.expectTerminalScopeIdentity(
                missingTransferContext,
                "TRANSFER",
                "transfer-open-without-close"
        );
        ledger.expectTerminalScopeIdentity(
                missingTransferContext,
                "ROUTE_CHILD",
                "route-child-expected"
        );
        ledger.recordScope(
                missingTransferContext,
                "ROUTE_CHILD",
                "route-child-unexpected",
                "WRONG_IDENTITY_CLOSE"
        );
        ledger.recordScope(
                missingTransferContext,
                "CANDIDATE_INVALIDATION",
                "candidate-invalidation-unexpected",
                "WRONG_CANDIDATE_IDENTITY_CLOSE"
        );
        ledger.expectTerminalScopeIdentity(
                missingTransferContext,
                "CANDIDATE_INVALIDATION",
                "candidate-invalidation-expected"
        );
        ledger.recordScope(
                missingTransferContext,
                "CANDIDATE_OBSERVATION_WITHOUT_EXPECTATION",
                "",
                "MISSING_CANDIDATE_IDENTITY"
        );
        ledger.recordScope(
                missingTransferContext,
                "PER_ITEM_ROOT",
                "implicit-item-root",
                "NATURAL_FINISH"
        );
        ledger.recordMaintenanceTerminal(missingTransferMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(missingTransferMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(missingTransferMaintenance, "REARM", "WAIT_FOR_REARM");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord missingTransferCoverage =
                ledger.recordCoverageClose(
                        missingTransferMaintenance,
                        "NO_USER_TASK_COVERAGE_CLOSE"
                );
        assertFalse(missingTransferCoverage.snapshot().lifecycleCoverageComplete());
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("EXPECTED_TRANSFER_TERMINALS(expected=1,observed=0)"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("MISSING_ROUTE_CHILD_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNEXPECTED_ROUTE_CHILD_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("MISSING_CANDIDATE_INVALIDATION_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNEXPECTED_CANDIDATE_INVALIDATION_TERMINAL_IDENTITIES"));
        assertTrue(missingTransferCoverage.snapshot().missingLifecycleBoundaries()
                .contains("UNAVAILABLE_TERMINAL_IDENTITIES"));
        assertFalse(missingTransferCoverage.snapshot().terminalIdentityCoverageComplete());
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: unavailable expected identity records a coverage gap")
    void unavailableExpectedIdentityRecordsCoverageGap() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task unavailableExpectedMaintenance = new TestTask("maintenance-unavailable-expected-identity");
        StoreDepositAutomaticContext unavailableExpectedContext = ledger.beginRun(
                unavailableExpectedMaintenance,
                null,
                82L
        );
        ledger.expectTerminalScopeIdentity(
                unavailableExpectedContext,
                "TRANSFER",
                ""
        );
        ledger.recordScope(
                unavailableExpectedContext,
                "PER_ITEM_ROOT",
                "implicit-item-root",
                "NATURAL_FINISH"
        );
        ledger.recordMaintenanceTerminal(unavailableExpectedMaintenance, "DONE", "RUNNING");
        ledger.recordPressureOwnedRunClose(unavailableExpectedMaintenance, "OWNED_RUN_DONE");
        ledger.recordRunToWait(unavailableExpectedMaintenance, "REARM", "WAIT_FOR_REARM");
        StoreDepositAutomaticLifecycleLedger.TerminalRecord unavailableExpectedCoverage =
                ledger.recordCoverageClose(
                        unavailableExpectedMaintenance,
                        "NO_USER_TASK_COVERAGE_CLOSE"
                );
        assertFalse(unavailableExpectedCoverage.snapshot().lifecycleCoverageComplete());
        assertFalse(unavailableExpectedCoverage.snapshot().terminalIdentityCoverageComplete());
        assertTrue(unavailableExpectedCoverage.snapshot().missingLifecycleBoundaries()
                .contains("EXPECTED_TERMINAL_IDENTITY|TRANSFER|UNAVAILABLE"));
    }
}
