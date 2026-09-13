package lavi.minecraft.diagnostics.mining.gold.root;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

//20260913_kpopmodder: Same-root yield and a mismatched assignment cannot consume the original gold terminal reservation.
class GoldUserRootObservationTest {
    @AfterEach
    void resetDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void onlyExactRootRemovalClosesTheGoldScopeOnce() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Object instance = new Object();
        Object world = new Object();
        ObservationActivation activation = new ObservationActivation(
                ChatClefDiagnostics.diagnosticActivationEpoch(), 1, instance, world);
        ObservationScope scope = new ObservationScope(activation, "mining", "old-request", "");
        GoldUserRootObservation observer = new GoldUserRootObservation();
        Task root = new RootTask();
        observer.bind(root, "assignment-1", scope, () -> new Object[]{"preparationReentries", 80});

        observer.removed(root, root, "assignment-1", "SAME_ROOT_YIELD");
        assertTrue(scope.isCurrent());
        observer.removed(root, null, "assignment-2", "MISMATCHED_ASSIGNMENT");
        assertTrue(scope.isCurrent());
        observer.assigned(root, root, "assignment-1", "assignment-2");
        assertTrue(scope.isCurrent());
        observer.removed(root, null, "assignment-1", "OLD_ASSIGNMENT_AFTER_RETAINED_ROOT");
        assertTrue(scope.isCurrent());
        observer.removed(root, null, "assignment-2", "USER_ROOT_CANCEL_STOP_RETURNED");
        assertFalse(scope.isCurrent());
        long attempts = ((Number) scope.ledger().summary()[3]).longValue();
        assertEquals(1L, attempts);
        observer.removed(root, null, "assignment-2", "DUPLICATE_FINISH_AFTER_CANCEL");
        assertEquals(attempts, ((Number) scope.ledger().summary()[3]).longValue());
    }

    private static final class RootTask extends Task {
        @Override protected void onStart() { }
        @Override protected Task onTick() { return null; }
        @Override protected void onStop(Task interruptTask) { }
        @Override protected boolean isEqual(Task task) { return task == this; }
        @Override protected String toDebugString() { return "gold-root-test"; }
    }
}
