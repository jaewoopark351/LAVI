package lavi.minecraft.diagnostics.session.lifecycle.registration;

//20260913_kpopmodder: Expected diagnostic admission failures are values, never initializer errors.
public enum DiagnosticObserverRegistrationStatus {
    UNREGISTERED,
    REGISTERED,
    ALREADY_REGISTERED,
    CAPACITY_EXHAUSTED,
    INVALID_OBSERVER
}
