package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.effect.StoreDepositEffectDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.route.StoreDepositMovementDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.context;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositAutomaticContextIsolationContractTest {

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
    @DisplayName("scenario 16 [assertions 23-24]: absent automatic context is a no-op and manual entry attribution is unchanged")
    void sharedObserversNoOpWithoutAutomaticContextAndManualSourcesStayManual() throws IOException {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task manualRoot = new TestTask("manual-root");
        Task candidate = new TestTask("candidate");
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositOperationState manualState = bindings.registerRoot(
                manualRoot, "BARE_DEPOSIT_ALL_COMMAND"
        );
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        ContainerStoredTracker manualTracker = new ContainerStoredTracker(slot -> true);
        bindings.bindTracker(manualRoot, manualTracker, "ROOT_ANY_CONTAINER", null);
        StoreDepositEffectDiagnostics effects = new StoreDepositEffectDiagnostics(bindings, gate);
        StoreDepositAutomaticTerminalDiagnostics terminals = new StoreDepositAutomaticTerminalDiagnostics(
                new StoreDepositAutomaticLifecycleLedger(), gate
        );
        StoreDepositMovementDiagnostics movement = new StoreDepositMovementDiagnostics(bindings, gate, terminals);
        movement.stageRouteCandidate(manualRoot, candidate, new BlockPos(1, 64, 2), 1, true);
        movement.observeMovementResult(
                manualRoot, new BlockPos(1, 64, 2), "MOVEMENT_PROGRESS_FAILED", true, false, true
        );

        StoreDepositTransferAttemptRegistry attempts = new StoreDepositTransferAttemptRegistry(bindings, terminals);
        StoreDepositSlotActionDiagnostics slots = new StoreDepositSlotActionDiagnostics(attempts, gate, terminals);
        slots.beginAction(candidate, new Object(), 7, 9, 0, SlotActionType.PICKUP, null);

        assertFalse(manualState.context().isAutomaticDepositOperation());
        assertFalse(manualState.automaticContext().available());
        assertFalse(effects.hasAutomaticContext(manualTracker));
        assertEquals(0L, manualState.routeState().selectedCandidateGeneration());
        assertEquals("UNAVAILABLE", slots.currentSlotActionId());

        StoreDepositOperationContext depositAll = context("BARE_DEPOSIT_ALL_COMMAND");
        StoreDepositOperationContext deposit = context("BARE_DEPOSIT_COMMAND");
        StoreDepositOperationContext storeHome = context("STORE_HOME");
        assertTrue(depositAll.isDepositAllOperation());
        assertFalse(depositAll.isAutomaticDepositOperation());
        assertFalse(deposit.isDepositAllOperation());
        assertFalse(deposit.isAutomaticDepositOperation());
        assertFalse(storeHome.isDepositAllOperation());
        assertFalse(storeHome.isAutomaticDepositOperation());

        String variants = source(
                "src/main/java/lavi/minecraft/diagnostics/command/deposit/DepositCommandVariant.java"
        );
        String storeHomeCommand = source(
                "src/main/java/lavi/minecraft/task/container/home/command/StoreHomeCommand.java"
        );
        String storedTracker = source(
                "src/main/java/adris/altoclef/tasks/container/ContainerStoredTracker.java"
        );
        assertTrue(variants.contains("DEPOSIT(\"deposit\", \"BARE_DEPOSIT_COMMAND\")"));
        assertTrue(variants.contains("DEPOSIT_ALL(\"deposit_all\", \"BARE_DEPOSIT_ALL_COMMAND\")"));
        assertFalse(storeHomeCommand.contains("StoreDepositDiagnostics"));
        int automaticGate = storedTracker.indexOf(
                "boolean automaticDiagnostics = diagnosticsEnabled"
        );
        int manualObservation = storedTracker.indexOf(
                "if (diagnosticsEnabled && !automaticDiagnostics)",
                automaticGate
        );
        int trackedMutation = storedTracker.indexOf(
                "if (!playerInventorySlot && acceptPredicateResult)", manualObservation
        );
        int automaticObservation = storedTracker.indexOf("if (automaticDiagnostics)", trackedMutation);
        assertTrue(automaticGate >= 0);
        assertTrue(manualObservation > automaticGate);
        assertTrue(trackedMutation > manualObservation);
        assertTrue(automaticObservation > trackedMutation);
        assertTrue(storedTracker.contains("String trackerTotalBefore = automaticDiagnostics"));
        int unstoredMethod = storedTracker.indexOf("getUnstoredItemTargetsYouCanStore");
        int unstoredBoundaryGate = storedTracker.indexOf(
                "boolean automaticDiagnostics = ChatClefDiagnostics.isBoundaryEnabled()",
                unstoredMethod
        );
        int unstoredLoop = storedTracker.indexOf("for (ItemTarget target : toStore)", unstoredBoundaryGate);
        int diagnosticCount = storedTracker.indexOf("? diagnosticStoredCount", unstoredLoop);
        int automaticNotStoredObservation = storedTracker.indexOf(
                "StoreDepositDiagnostics.observeAutomaticNotStoredDecision(",
                diagnosticCount
        );
        assertTrue(unstoredBoundaryGate > unstoredMethod);
        assertTrue(unstoredLoop > unstoredBoundaryGate);
        assertTrue(diagnosticCount > unstoredLoop);
        assertTrue(automaticNotStoredObservation > diagnosticCount);

        String storeDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java"
        );
        String pressureChain = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChain.java"
        );
        String maintenanceTask = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java"
        );
        String maintenanceDiagnostics = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/diagnostics/AutoDepositMaintenanceDiagnostics.java"
        );
        assertFalse(storeDiagnostics.contains("diagnosticPlayerPosition"));
        assertFalse(storeDiagnostics.contains("mod.getWorld()"));
        assertTrue(pressureChain.contains("if (task.diagnosticAutomaticRunEnabled())"));
        assertTrue(pressureChain.contains("if (activeDiagnosticMaintenanceTask != null)"));
        assertTrue(maintenanceTask.contains("diagnostics.recordTerminal("));
        assertTrue(maintenanceDiagnostics.contains("terminalRecorded"));
        assertTrue(maintenanceDiagnostics.contains("!ChatClefDiagnostics.isBoundaryEnabled()"));
    }
}
