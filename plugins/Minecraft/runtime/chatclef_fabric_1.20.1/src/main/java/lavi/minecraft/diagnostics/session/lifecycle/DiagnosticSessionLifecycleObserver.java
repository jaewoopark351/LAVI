package lavi.minecraft.diagnostics.session.lifecycle;

//20260831_kpopmodder: Observe diagnostics-session lifecycle without owning gameplay lifecycle.
public interface DiagnosticSessionLifecycleObserver {
    default void beforeModeOff() {
    }

    default void afterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
    }

    default Object[] finalSnapshotFields() {
        return new Object[0];
    }
}
