package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;

//20260913_kpopmodder: Resolve command/instance ownership independently of pressure capture and emission.
public final class AutoDepositObservationScopeResolver {
    private AutoDepositObservationScopeResolver() {
    }

    public static ObservationScope open(Task task) {
        if (task instanceof AutoDepositObservationOwner owner) return owner.diagnosticObservationScope();
        return captureCurrent(task);
    }

    public static ObservationScope captureCurrent(Task task) {
        AltoClef mod = AltoClef.getInstance();
        ObservationActivation activation = ObservationDiagnostics.captureActivation(
                mod, mod == null ? null : mod.getWorld());
        Object[] context = ChatClefDiagnostics.currentCommandContextFields();
        return ObservationDiagnostics.open(activation, "deposit", AutoDepositObservationFields.requestId(context),
                AutoDepositObservationFields.concat(context, new Object[]{
                        "observationTask", AutoDepositObservationFields.identity(task),
                        "scopeSource", "automatic_pressure",
                        "commandContextBinding", "CAPTURED_CONTEXT_AT_SCOPE_CREATION"}));
    }
}
