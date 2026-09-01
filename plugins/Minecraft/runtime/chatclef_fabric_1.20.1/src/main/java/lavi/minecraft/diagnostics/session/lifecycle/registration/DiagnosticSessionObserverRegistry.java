package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//20260831_kpopmodder: Own only bounded diagnostic lifecycle observer registration.
public final class DiagnosticSessionObserverRegistry {
    private static final int MAX_OBSERVERS = 16;

    private final List<DiagnosticSessionLifecycleObserver> observers = new ArrayList<>();

    public synchronized void register(DiagnosticSessionLifecycleObserver observer) {
        Objects.requireNonNull(observer, "observer");
        if (observers.contains(observer)) {
            return;
        }
        if (observers.size() >= MAX_OBSERVERS) {
            throw new IllegalStateException("Diagnostic session lifecycle observer capacity exhausted.");
        }
        observers.add(observer);
    }

    public synchronized List<DiagnosticSessionLifecycleObserver> snapshot() {
        return List.copyOf(observers);
    }
}
