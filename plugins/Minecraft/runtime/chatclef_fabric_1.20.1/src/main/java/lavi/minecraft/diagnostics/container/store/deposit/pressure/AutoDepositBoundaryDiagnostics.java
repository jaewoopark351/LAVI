package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;

//20260913_kpopmodder: Share one bounded deposit observation scope per command, never per tick or child.
public final class AutoDepositBoundaryDiagnostics {
    private AutoDepositBoundaryDiagnostics() {
    }

    public static void log(String event, String reason, Task task, Object... fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) return;
        try {
            ObservationScope scope = AutoDepositObservationScopeResolver.open(task);
            scope.record(event, reason, AutoDepositObservationFields.fingerprint(event, reason, fields), false,
                    AutoDepositObservationFields.freezeValues(fields));
        } catch (RuntimeException | LinkageError ignored) {
            // Optional diagnostic failure cannot consume an engine exception or select behavior.
            ObservationDiagnostics.captureFailed("deposit", "BOUNDARY_CAPTURE_FAILED");
        }
    }

}
