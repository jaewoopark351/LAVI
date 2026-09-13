package lavi.minecraft.diagnostics.toolselect.call;

import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticRegistrationFailures;

import java.util.function.Supplier;

//20260913_kpopmodder: Isolate diagnostic class initialization from the caller's unchanged game action.
public final class ToolDiagnosticInvocation {
    private final Runnable disableDiagnosticOwner;
    private final String ownerId;
    private volatile boolean disabled;

    public ToolDiagnosticInvocation(String ownerId, Runnable disableDiagnosticOwner) {
        this.ownerId = ownerId;
        this.disableDiagnosticOwner = disableDiagnosticOwner;
    }

    public void run(Runnable diagnostic) {
        call(() -> {
            diagnostic.run();
            return null;
        }, null);
    }

    public synchronized <T> T call(Supplier<T> diagnostic, T fallback) {
        if (disabled) {
            return fallback;
        }
        try {
            return diagnostic.get();
        } catch (RuntimeException | LinkageError failure) {
            disabled = true;
            try {
                disableDiagnosticOwner.run();
            } catch (RuntimeException | LinkageError unavailableOwner) {
                // An erroneous class can reject cleanup too; this invocation remains permanently disabled.
            }
            try {
                DiagnosticRegistrationFailures.reportInitializationFailure(ownerId, "TOOL_DIAGNOSTIC_CALL", failure);
            } catch (RuntimeException | LinkageError unavailableFailureSink) {
                // The emergency sink must never replace the original game action either.
            }
            return fallback;
        }
    }

    public boolean isDisabled() {
        return disabled;
    }
}
