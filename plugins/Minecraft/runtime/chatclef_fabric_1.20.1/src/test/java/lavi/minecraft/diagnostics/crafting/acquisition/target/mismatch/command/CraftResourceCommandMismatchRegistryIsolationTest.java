package lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Prevent reused correlations from mixing exact command mismatch evidence.
class CraftResourceCommandMismatchRegistryIsolationTest {
    @Test
    void exactScopesRemainIsolatedWhileTheSessionBudgetSurvivesRetirement() {
        CraftResourceTargetDiagnosticsRegistry registry =
                new CraftResourceTargetDiagnosticsRegistry();
        IronPickaxeAcquisitionScopeKey scopeA = scope("request-a", "root-a");
        IronPickaxeAcquisitionScopeKey scopeB = scope("request-b", "root-b");
        assertTrue(registry.activateScope(scopeA));
        assertTrue(registry.activateScope(scopeB));

        CraftResourceMismatchDecision decisionA = registry.evaluateMismatch(
                scopeA,
                mismatch("0,64,0")
        );
        CraftResourceMismatchDecision decisionB = registry.evaluateMismatch(
                scopeB,
                mismatch("1,64,0")
        );
        assertTrue(decisionA.emissionRequested());
        assertTrue(decisionB.emissionRequested());
        assertTrue(registry.recordMismatchAdmissionDenied(
                scopeA,
                decisionA.fingerprint()
        ));

        CraftResourceCommandMismatchSnapshot snapshotA = registry
                .commandMismatchSnapshot(scopeA)
                .orElseThrow();
        CraftResourceCommandMismatchSnapshot snapshotB = registry
                .commandMismatchSnapshot(scopeB)
                .orElseThrow();
        assertEquals(1, snapshotA.ownedMismatchOccurrenceCount());
        assertEquals(1, snapshotA.admissionDeniedCount());
        assertEquals(0, snapshotA.emissionFailureCount());
        assertEquals(1, snapshotB.ownedMismatchOccurrenceCount());
        assertEquals(0, snapshotB.admissionDeniedCount());
        assertEquals(0, snapshotB.emissionFailureCount());

        assertTrue(registry.retireScope(scopeA));
        assertTrue(registry.commandMismatchSnapshot(scopeA).isEmpty());
        assertTrue(registry.commandMismatchSnapshot(scopeB).isPresent());
        assertEquals(
                2,
                registry.snapshot().mismatchSnapshot().globalRetainedSignatureCount()
        );
    }

    private static IronPickaxeAcquisitionScopeKey scope(
            String requestId,
            String rootAssignmentId) {
        return new IronPickaxeAcquisitionScopeKey(
                "session",
                7,
                requestId,
                "reused-correlation",
                rootAssignmentId,
                11,
                rootAssignmentId + "-instance"
        );
    }

    private static CraftResourceMismatchObservation mismatch(String position) {
        return new CraftResourceMismatchObservation(
                "reused-correlation",
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                position,
                Optional.of(List.of("minecraft:iron_ore")),
                Optional.of("minecraft:chest"),
                "MINE_TARGET_GOAL_REQUEST",
                50,
                "task-instance"
        );
    }
}
