package lavi.minecraft.diagnostics.session.lifecycle.registration;

//20260913_kpopmodder: Report the committed registration state without claiming physical log persistence.
public record DiagnosticObserverRegistrationResult(
        DiagnosticObserverRegistrationStatus status, int registeredCount, int capacity, int addedCount) {
    public boolean accepted() {
        return status == DiagnosticObserverRegistrationStatus.REGISTERED
                || status == DiagnosticObserverRegistrationStatus.ALREADY_REGISTERED;
    }
}
