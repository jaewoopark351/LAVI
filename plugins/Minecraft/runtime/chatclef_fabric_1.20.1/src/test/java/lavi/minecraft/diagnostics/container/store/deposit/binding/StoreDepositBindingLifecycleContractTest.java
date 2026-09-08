package lavi.minecraft.diagnostics.container.store.deposit.binding;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.automaticContext;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.effectFields;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.fields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositBindingLifecycleContractTest {

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
    @DisplayName("scenario 6 [assertion 8]: tracker subscription generation and active-at-mutation state")
    void trackerResubscriptionAdvancesGenerationWithoutLosingActiveState() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        Task root = new TestTask("root");
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        bindings.registerRoot(root, "AUTO_DEPOSIT_ALL_CHAIN", automaticContext());
        bindings.bindTracker(root, tracker, "ROOT_ANY_CONTAINER", null);

        TrackerBinding initial = bindings.trackerBinding(tracker);
        TrackerBinding firstStart = bindings.markTrackerSubscriptionStarted(tracker);
        TrackerBinding stopped = bindings.markTrackerSubscriptionStopped(tracker);
        TrackerBinding secondStart = bindings.markTrackerSubscriptionStarted(tracker);

        assertEquals(0L, initial.subscriptionGeneration());
        assertFalse(initial.subscriptionActive());
        assertEquals(1L, firstStart.subscriptionGeneration());
        assertTrue(firstStart.subscriptionActive());
        assertEquals(1L, stopped.subscriptionGeneration());
        assertFalse(stopped.subscriptionActive());
        assertEquals(2L, secondStart.subscriptionGeneration());
        assertTrue(secondStart.subscriptionActive());
        Map<String, Object> activeMutation = fields(effectFields(secondStart, true));
        Map<String, Object> inactiveMutation = fields(effectFields(stopped, true));
        assertEquals(2L, activeMutation.get("subscriptionGeneration"));
        assertEquals(true, activeMutation.get("subscriptionActiveAtMutation"));
        assertEquals(false, inactiveMutation.get("subscriptionActiveAtMutation"));
        assertEquals("1,64,2", activeMutation.get("lastBlockPosInteractionAtEvent"));
        assertEquals(true, activeMutation.get("predicateEvaluated"));
        assertEquals(true, activeMutation.get("predicateResult"));
    }
}
