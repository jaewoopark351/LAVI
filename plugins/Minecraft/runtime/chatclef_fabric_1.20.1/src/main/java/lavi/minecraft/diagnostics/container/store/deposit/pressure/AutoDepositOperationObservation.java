package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;

//20260913_kpopmodder: Freeze scope at operation creation; late results never rebind to the current command.
public final class AutoDepositOperationObservation {
    private final ObservationScope scope;

    public AutoDepositOperationObservation(Task originalRoot) {
        ObservationScope captured = ObservationScope.NOOP;
        if (ChatClefDiagnostics.isBoundaryEnabled()) {
            try { captured = AutoDepositObservationScopeResolver.captureCurrent(originalRoot); }
            catch (RuntimeException | LinkageError failure) {
                ObservationDiagnostics.captureFailed("deposit", "OPERATION_BINDING_CAPTURE_FAILED");
            }
        }
        scope = captured;
    }

    AutoDepositOperationObservation(ObservationScope captured) {
        scope = captured == null ? ObservationScope.NOOP : captured;
    }

    public ObservationScope scope() {
        if (ChatClefDiagnostics.isBoundaryEnabled() && !scope.isCurrent()) {
            ObservationDiagnostics.captureFailed("deposit", "STALE_OR_UNAVAILABLE_OPERATION_SCOPE");
        }
        return scope;
    }
}
