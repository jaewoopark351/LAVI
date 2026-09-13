package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

//20260913_kpopmodder: A late operation keeps its old scope and cannot silently attach to a later activation.
class AutoDepositOperationObservationTest {
    @AfterEach
    void resetDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void lateResultRetainsOriginalScopeAndInvalidationNeverRebindsIt() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Object instance = new Object();
        Object world = new Object();
        ObservationActivation activation = new ObservationActivation(
                ChatClefDiagnostics.diagnosticActivationEpoch(), 1, instance, world);
        ObservationScope original = new ObservationScope(activation, "deposit", "old_request", "");
        AutoDepositOperationObservation handle = new AutoDepositOperationObservation(original);
        ObservationScope replacement = new ObservationScope(activation, "deposit", "new_request", "");

        assertSame(original, handle.scope());
        assertEquals("old_request", handle.scope().operationKey());
        assertEquals("new_request", replacement.operationKey());
        activation.invalidate();
        assertSame(original, handle.scope());
        assertFalse(handle.scope().isCurrent());
    }
}
