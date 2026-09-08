package lavi.minecraft.task.container.deposit.handoff;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class DepositAllPostPlaceHandoffSourceContractTest {

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
    @DisplayName("automatic post-place handoff is one tick and manual deposit remains unchanged")
    void automaticPostPlaceHandoffIsScopedToTheGeneralMaintenanceFactory() throws IOException {
        String depositAll = source(
                "src/main/java/adris/altoclef/tasks/container/DepositAllTask.java"
        );
        String generalFactory = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/child/AutoDepositGeneralTaskFactory.java"
        );

        int selectedTargetRead = depositAll.indexOf(
                "Optional<BlockPos> selectedTarget = _targetState.selectedTarget();"
        );
        int handoffGate = depositAll.indexOf(
                "if (deferAfterCompletedPlacement())",
                selectedTargetRead
        );
        int openExistingBranch = depositAll.indexOf(
                "if (selectedTarget.isPresent())",
                handoffGate
        );
        int placementOwner = depositAll.indexOf(
                "return _placementTaskOwner.getOrCreate(",
                openExistingBranch
        );

        assertTrue(selectedTargetRead >= 0);
        assertTrue(handoffGate > selectedTargetRead);
        assertTrue(openExistingBranch > handoffGate);
        assertTrue(placementOwner > openExistingBranch);
        assertTrue(depositAll.contains("DepositAllPlacementTaskOwner.ephemeral()"));
        assertTrue(depositAll.contains("DepositAllPostPlaceHandoff.disabled()"));
        assertTrue(generalFactory.contains("DepositAllPlacementTaskOwner::retaining"));
        assertTrue(generalFactory.contains("DepositAllPostPlaceHandoff::singleTick"));
        assertFalse(depositAll.contains("CarryOn"));
        assertFalse(depositAll.contains("Input.SNEAK"));
        assertFalse(depositAll.contains("cancelEverything"));
    }
}
