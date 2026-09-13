package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.ArrayList;
import java.util.List;

//20260831_kpopmodder: Own only bounded diagnostic lifecycle observer registration.
public final class DiagnosticSessionObserverRegistry {
    //20260913_kpopmodder: Admit the complete 17-observer composition under a fixed bound.
    public static final int MAX_OBSERVERS = 32;

    private final List<DiagnosticRegisteredObserver> observers = new ArrayList<>();

    public DiagnosticObserverRegistrationResult register(DiagnosticSessionLifecycleObserver observer) {
        DiagnosticObserverRegistrationResult result = registerAtomically(null,
                new DiagnosticSessionLifecycleObserver[]{observer});
        if (!result.accepted()) {
            DiagnosticRegistrationFailures.report("LEGACY_OBSERVER", result, "REGISTER", result.status().name());
        }
        return result;
    }

    //20260913_kpopmodder: Validate an owner's entire group before publishing any callback.
    public DiagnosticObserverRegistrationResult registerOwner(
            DiagnosticOwnerRegistration owner, DiagnosticSessionLifecycleObserver... requiredObservers) {
        if (owner == null) {
            DiagnosticObserverRegistrationResult result = rejected(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER);
            DiagnosticRegistrationFailures.report("MISSING_OWNER", result, "REGISTER_OWNER", "INVALID_OWNER");
            return result;
        }
        return owner.registerWith(() -> registerAtomically(owner, requiredObservers));
    }

    private synchronized DiagnosticObserverRegistrationResult registerAtomically(
            DiagnosticOwnerRegistration owner, DiagnosticSessionLifecycleObserver[] requested) {
        if (requested == null || requested.length == 0 || (owner != null && !owner.hasValidOwnerId())) {
            return rejected(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER);
        }
        List<DiagnosticSessionLifecycleObserver> additions = new ArrayList<>();
        try {
            for (DiagnosticSessionLifecycleObserver observer : requested) {
                if (observer == null) {
                    return rejected(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER);
                }
                DiagnosticRegisteredObserver existing = existing(observer);
                if (existing != null) {
                    // An owner name never gives another owner access to an existing callback.
                    if (owner != null && existing.owner() != owner) {
                        return rejected(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER);
                    }
                } else if (!additions.contains(observer)) {
                    if (additions.size() >= MAX_OBSERVERS - observers.size()) {
                        return rejected(DiagnosticObserverRegistrationStatus.CAPACITY_EXHAUSTED);
                    }
                    additions.add(observer);
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // A broken diagnostic equals implementation is invalid input, not a game failure.
            return rejected(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER);
        }
        if (additions.size() > MAX_OBSERVERS - observers.size()) {
            return rejected(DiagnosticObserverRegistrationStatus.CAPACITY_EXHAUSTED);
        }
        if (owner != null && !additions.isEmpty()
                && observers.stream().anyMatch(registered -> registered.owner() == owner)) {
            // Required callbacks must be declared together; a later expansion is not a partial owner.
            return rejected(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER);
        }
        for (DiagnosticSessionLifecycleObserver observer : additions) {
            observers.add(new DiagnosticRegisteredObserver(observer, owner));
        }
        return new DiagnosticObserverRegistrationResult(additions.isEmpty()
                ? DiagnosticObserverRegistrationStatus.ALREADY_REGISTERED
                : DiagnosticObserverRegistrationStatus.REGISTERED,
                observers.size(), MAX_OBSERVERS, additions.size());
    }

    private DiagnosticRegisteredObserver existing(DiagnosticSessionLifecycleObserver observer) {
        for (DiagnosticRegisteredObserver registered : observers) {
            // Preserve ArrayList.contains' candidate.equals(existing) contract.
            if (observer.equals(registered.observer())) {
                return registered;
            }
        }
        return null;
    }

    private synchronized DiagnosticObserverRegistrationResult rejected(DiagnosticObserverRegistrationStatus status) {
        return new DiagnosticObserverRegistrationResult(status, observers.size(), MAX_OBSERVERS, 0);
    }

    public synchronized int registeredCount() {
        return observers.size();
    }

    public synchronized List<DiagnosticSessionLifecycleObserver> snapshot() {
        List<DiagnosticSessionLifecycleObserver> callbacks = new ArrayList<>(observers.size());
        for (DiagnosticRegisteredObserver observer : observers) {
            callbacks.add(observer.callback());
        }
        return List.copyOf(callbacks);
    }
}
