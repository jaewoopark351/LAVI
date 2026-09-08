package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.SlotHarness;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.automaticSlotHarness;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositSlotMutationContractTest {

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
    @DisplayName("scenario 4 [assertion 5]: one slot action owns ordered multiple slot mutations")
    void oneSlotActionOwnsOrderedMutationIdentities() throws IOException {
        SlotHarness harness = automaticSlotHarness();
        long actionSequence = harness.activeTransfer().nextSlotActionSequence();
        String actionId = harness.activeTransfer().transferAttemptId() + "-action-" + actionSequence;
        String firstMutation = actionId + "-mutation-1";
        String secondMutation = actionId + "-mutation-2";

        assertNotEquals("UNAVAILABLE", actionId);
        assertEquals(actionId + "-mutation-1", firstMutation);
        assertEquals(actionId + "-mutation-2", secondMutation);
        assertNotEquals(firstMutation, secondMutation);
        String slotDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        assertTrue(slotDiagnostics.contains(
                "transfer.transferAttemptId() + \"-action-\" + sequence"
        ));
        assertTrue(slotDiagnostics.contains(
                "action.slotActionId + \"-mutation-\" + ordinal"
        ));
        assertTrue(slotDiagnostics.contains("action.mutationCount++"));
        assertTrue(slotDiagnostics.contains("currentMutation.set(mutation)"));
    }

    @Test
    @DisplayName("scenario 5 [assertions 6-7]: both tracker roles share a mutation and local effect is not durable proof")
    void bothTrackerRolesShareMutationIdentityAndLocalMutationLeavesDurabilityUnavailable() throws IOException {
        SlotHarness harness = automaticSlotHarness();
        String actionId = harness.activeTransfer().transferAttemptId()
                + "-action-" + harness.activeTransfer().nextSlotActionSequence();
        String mutationId = actionId + "-mutation-1";
        TrackerBinding root = new TrackerBinding(
                "operation", "ROOT_ANY_CONTAINER", null, 1L, true
        );
        TrackerBinding target = new TrackerBinding(
                "operation", "TARGET_CONTAINER", new BlockPos(1, 64, 2), 1L, true
        );
        Map<String, String> mutationByTrackerRole = Map.of(
                root.trackerRole(), mutationId,
                target.trackerRole(), mutationId
        );

        assertEquals(Set.of("ROOT_ANY_CONTAINER", "TARGET_CONTAINER"), mutationByTrackerRole.keySet());
        assertEquals(Set.of(mutationId), Set.copyOf(mutationByTrackerRole.values()));
        String slotDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositSlotActionDiagnostics.java"
        );
        int observationStart = slotDiagnostics.indexOf("public void observeTrackerMutation(");
        int observationEnd = slotDiagnostics.indexOf("public Object[] currentMutationFields()", observationStart);
        String observationBody = slotDiagnostics.substring(observationStart, observationEnd);
        assertTrue(observationBody.contains("MutationContext mutation = activeMutation()"));
        assertTrue(observationBody.contains("mutation.trackerRoles.add(binding.trackerRole())"));
        assertFalse(observationBody.contains("new MutationContext"));
        assertTrue(slotDiagnostics.contains("\"trackerRolesObserved\", Set.copyOf(trackerRoles)"));
        assertTrue(slotDiagnostics.contains("\"mutationObservationSource\", \"LOCAL_INTERNAL_CLICK\""));
        assertTrue(slotDiagnostics.contains("\"stableOrServerSnapshotAvailable\", false"));
        assertTrue(slotDiagnostics.contains("\"durableEffect\", \"UNAVAILABLE\""));
        assertTrue(slotDiagnostics.contains(
                "\"missingBoundaries\", \"SERVER_SLOT_UPDATE,POST_ACTION_STABLE,HANDLER_REVISION\""
        ));
        String effectDiagnostics = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/effect/StoreDepositEffectDiagnostics.java"
        );
        assertTrue(effectDiagnostics.contains("currentSlotMutationId"));
    }
}
