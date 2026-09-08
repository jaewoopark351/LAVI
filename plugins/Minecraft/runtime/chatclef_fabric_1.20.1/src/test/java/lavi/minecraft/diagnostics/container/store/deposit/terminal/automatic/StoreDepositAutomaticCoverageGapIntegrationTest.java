package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.captureOutput;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.selection;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.target;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Split the automatic-ledger contract from transfer-registry coverage integration.
class StoreDepositAutomaticCoverageGapIntegrationTest {

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
    @DisplayName("scenario 14 [assertion 21b]: registry eviction is an explicit automatic coverage gap")
    void transferRegistryEvictionRemainsAnExplicitCoverageGap() throws IOException {
        StoreDepositAutomaticLifecycleLedger evictionCoverageLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositEmissionGate evictionCoverageGate = new StoreDepositEmissionGate();
        StoreDepositAutomaticTerminalDiagnostics evictionCoverageTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(
                        evictionCoverageLedger,
                        evictionCoverageGate
                );
        StoreDepositBindingRegistry evictionCoverageBindings = new StoreDepositBindingRegistry();
        Task evictionCoverageMaintenance = new TestTask("maintenance-transfer-registry-cap");
        Task evictionCoverageParent = new TestTask("transfer-registry-parent");
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositAutomaticContext evictionCoverageContext = evictionCoverageTerminals.beginRun(
                evictionCoverageMaintenance,
                null,
                83L
        );
        evictionCoverageBindings.registerRoot(
                evictionCoverageParent,
                "AUTO_DEPOSIT_ALL_CHAIN",
                evictionCoverageContext
        );
        StoreDepositTransferAttemptRegistry evictionCoverageRegistry =
                new StoreDepositTransferAttemptRegistry(
                        evictionCoverageBindings,
                        evictionCoverageTerminals
                );
        String evictionCoverageOutput = captureOutput(() -> {
            for (int index = 0; index < 257; index++) {
                Task candidateTask = new TestTask("transfer-registry-candidate-" + index);
                evictionCoverageRegistry.stage(
                        evictionCoverageParent,
                        candidateTask,
                        selection(target(10), 64)
                );
                evictionCoverageRegistry.reconcile(
                        evictionCoverageParent,
                        null,
                        candidateTask,
                        false,
                        true,
                        candidateTask
                );
            }
        });
        StoreDepositAutomaticLifecycleLedger.Snapshot evictionCoverageSnapshot =
                evictionCoverageLedger.snapshotFor(evictionCoverageMaintenance);
        assertEquals(0, evictionCoverageSnapshot.terminalCount("TRANSFER"));
        assertEquals(257, evictionCoverageSnapshot.expectedTerminalCount("TRANSFER"));
        assertFalse(evictionCoverageSnapshot.terminalIdentityCoverageComplete());
        assertTrue(evictionCoverageSnapshot.missingLifecycleBoundaries()
                .contains("TRANSFER_ATTEMPT_REGISTRY"));
        assertTrue(evictionCoverageOutput.contains("STORE_DEPOSIT_AUTOMATIC_COVERAGE_GAP"));
        assertTrue(evictionCoverageOutput.contains("DIAGNOSTIC_REGISTRY_EVICTED"));
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: staged transfer registry eviction remains an explicit gap")
    void stagedTransferRegistryEvictionRemainsAnExplicitCoverageGap() {
        StoreDepositAutomaticLifecycleLedger stagedEvictionLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositEmissionGate stagedEvictionGate = new StoreDepositEmissionGate();
        StoreDepositAutomaticTerminalDiagnostics stagedEvictionTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(
                        stagedEvictionLedger,
                        stagedEvictionGate
                );
        StoreDepositBindingRegistry stagedEvictionBindings = new StoreDepositBindingRegistry();
        Task stagedEvictionMaintenance = new TestTask("maintenance-staged-transfer-registry-cap");
        Task stagedEvictionParent = new TestTask("staged-transfer-registry-parent");
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositAutomaticContext stagedEvictionContext = stagedEvictionTerminals.beginRun(
                stagedEvictionMaintenance,
                null,
                84L
        );
        stagedEvictionBindings.registerRoot(
                stagedEvictionParent,
                "AUTO_DEPOSIT_ALL_CHAIN",
                stagedEvictionContext
        );
        StoreDepositTransferAttemptRegistry stagedEvictionRegistry =
                new StoreDepositTransferAttemptRegistry(
                        stagedEvictionBindings,
                        stagedEvictionTerminals
                );
        String stagedEvictionOutput = captureOutput(() -> {
            for (int index = 0; index < 257; index++) {
                stagedEvictionRegistry.stage(
                        stagedEvictionParent,
                        new TestTask("staged-transfer-registry-candidate-" + index),
                        selection(target(10), 64)
                );
            }
        });
        StoreDepositAutomaticLifecycleLedger.Snapshot stagedEvictionSnapshot =
                stagedEvictionLedger.snapshotFor(stagedEvictionMaintenance);
        assertEquals(0, stagedEvictionSnapshot.expectedTerminalCount("TRANSFER"));
        assertEquals(0, stagedEvictionSnapshot.terminalCount("TRANSFER"));
        assertFalse(stagedEvictionSnapshot.terminalIdentityCoverageComplete());
        assertTrue(stagedEvictionSnapshot.missingLifecycleBoundaries()
                .contains("STAGED_TRANSFER_ATTEMPT_REGISTRY"));
        assertTrue(stagedEvictionOutput.contains("STORE_DEPOSIT_AUTOMATIC_COVERAGE_GAP"));
    }

    @Test
    @DisplayName("scenario 14 [assertion 21]: terminal expectation registration precedes observations")
    void terminalExpectationRegistrationPrecedesObservations() throws IOException {
        String transferRegistry = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferAttemptRegistry.java"
        );
        String slotActions = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        String movement = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/route/StoreDepositMovementDiagnostics.java"
        );
        String childReconciliation = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/lifecycle/StoreDepositChildReconciliationDiagnostics.java"
        );
        assertTrue(transferRegistry.indexOf("active.put(candidateChild, promoted);")
                < transferRegistry.indexOf("\"TRANSFER\",\n                    transferAttemptId"));
        assertTrue(slotActions.contains("\"SLOT_ACTION\",\n                action.slotActionId"));
        assertTrue(slotActions.contains("\"SLOT_MUTATION\",\n                mutation.slotMutationId"));
        assertTrue(movement.contains("\"ROUTE_RECONCILIATION\","));
        int routeStateReconciliation = childReconciliation.indexOf(
                "state.routeState().recordChildReconciliation("
        );
        int routeChildExpectation = childReconciliation.indexOf(
                "movementDiagnostics.expectActiveRouteChildTerminal("
        );
        assertTrue(routeStateReconciliation >= 0);
        assertTrue(routeChildExpectation > routeStateReconciliation);
        assertTrue(movement.contains(
                "\"CANDIDATE_INVALIDATION\",\n                    generationBefore"
        ));
        assertTrue(movement.indexOf(
                "automaticTerminals.expectScopeIdentity(\n                    state.automaticContext(),\n                    \"CANDIDATE_INVALIDATION\""
        ) < movement.indexOf(
                "automaticTerminals.recordScopeIdentity(\n                    state.automaticContext(),\n                    \"CANDIDATE_INVALIDATION\""
        ));
    }
}
