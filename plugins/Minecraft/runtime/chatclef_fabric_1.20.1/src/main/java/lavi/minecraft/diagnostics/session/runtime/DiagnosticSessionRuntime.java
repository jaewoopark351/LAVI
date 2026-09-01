package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionRequest;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionToken;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionCoordinator;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionOutcome;
import lavi.minecraft.diagnostics.session.reset.DiagnosticSessionTestResetCoordinator;
import lavi.minecraft.diagnostics.session.reset.DiagnosticSessionTestResetResult;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

//20260831_kpopmodder: Serialize mode eligibility with shared admission while keeping I/O outside the accounting lock.
public final class DiagnosticSessionRuntime {
    private final DiagnosticModeController mode;
    private final ReentrantReadWriteLock eligibilityLock = new ReentrantReadWriteLock(true);
    private DiagnosticSessionAdmissionAuthority authority;
    private boolean cleanTeardownStarted;

    public DiagnosticSessionRuntime(DiagnosticModeController mode,
                                    DiagnosticSessionAdmissionAuthority authority) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.authority = Objects.requireNonNull(authority, "authority");
    }

    public DiagnosticDispatchResult dispatch(
            DiagnosticEventFamily family,
            String eventName,
            DiagnosticPhysicalEmission requestedEmission,
            DiagnosticCapPhysicalEmission canonicalCapEmission) {
        return dispatch(
                family,
                eventName,
                requestedEmission,
                canonicalCapEmission,
                DiagnosticDispatchObserver.NONE
        );
    }

    public DiagnosticDispatchResult dispatch(
            DiagnosticEventFamily family,
            String eventName,
            DiagnosticPhysicalEmission requestedEmission,
            DiagnosticCapPhysicalEmission canonicalCapEmission,
            DiagnosticDispatchObserver observer) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(requestedEmission, "requestedEmission");
        Objects.requireNonNull(canonicalCapEmission, "canonicalCapEmission");
        Objects.requireNonNull(observer, "observer");

        ReentrantReadWriteLock.ReadLock read = eligibilityLock.readLock();
        read.lock();
        try {
            DiagnosticSessionAdmissionAuthority current = authority;
            DiagnosticAdmissionDecision admission = current.admit(new DiagnosticAdmissionRequest(
                    family,
                    mode.isBoundaryEnabled() && !cleanTeardownStarted
            ));
            if (admission.rejectionReason()
                    != DiagnosticAdmissionDecision.RejectionReason.MODE_INELIGIBLE) {
                notifyAdmissionObserver(observer, admission);
            }
            DiagnosticEmissionOutcome capOutcome = null;
            if (admission.canonicalCapEventClaimedByThisDecision()) {
                DiagnosticCapEventContext capContext = new DiagnosticCapEventContext(
                        admission.newlyClaimedCapTrigger(),
                        eventName,
                        family,
                        admission.snapshot()
                );
                capOutcome = emit(
                        current,
                        admission.canonicalCapToken(),
                        () -> canonicalCapEmission.emit(capContext)
                );
            }
            DiagnosticEmissionOutcome requestedOutcome = admission.admitted()
                    ? emit(current, admission.token(), requestedEmission)
                    : null;
            if (requestedOutcome != null) {
                notifySettlementObserver(observer, requestedOutcome);
            }
            return new DiagnosticDispatchResult(admission, requestedOutcome, capOutcome);
        } finally {
            read.unlock();
        }
    }

    public boolean runIfEligible(Runnable action) {
        Objects.requireNonNull(action, "action");
        ReentrantReadWriteLock.ReadLock read = eligibilityLock.readLock();
        read.lock();
        try {
            if (!mode.isBoundaryEnabled() || cleanTeardownStarted) {
                return false;
            }
            action.run();
            return true;
        } finally {
            read.unlock();
        }
    }

    public <T> T callIfEligible(Supplier<T> action, T ineligibleValue) {
        Objects.requireNonNull(action, "action");
        ReentrantReadWriteLock.ReadLock read = eligibilityLock.readLock();
        read.lock();
        try {
            if (!mode.isBoundaryEnabled() || cleanTeardownStarted) {
                return ineligibleValue;
            }
            return action.get();
        } finally {
            read.unlock();
        }
    }

    public boolean isEligible() {
        ReentrantReadWriteLock.ReadLock read = eligibilityLock.readLock();
        read.lock();
        try {
            return mode.isBoundaryEnabled() && !cleanTeardownStarted;
        } finally {
            read.unlock();
        }
    }

    public void setBoundaryEnabled(boolean enabled) {
        setBoundaryEnabled(enabled, () -> {
        });
    }

    public void setBoundaryEnabled(boolean enabled, Runnable beforeOffPublication) {
        Objects.requireNonNull(beforeOffPublication, "beforeOffPublication");
        if (eligibilityLock.getReadHoldCount() > 0
                && !eligibilityLock.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException(
                    "A diagnostics eligibility callback cannot upgrade its read lease to a mode-transition write lease."
            );
        }
        ReentrantReadWriteLock.WriteLock write = eligibilityLock.writeLock();
        write.lock();
        try {
            boolean transitioningToOff = !enabled && !mode.isOff();
            if (transitioningToOff) {
                try {
                    beforeOffPublication.run();
                } catch (RuntimeException | LinkageError ignored) {
                    // Mode publication cannot be blocked by diagnostics-only cleanup.
                }
                mode.beginOffTransition();
            }
            mode.completeBoundaryTransition(enabled);
        } finally {
            write.unlock();
        }
    }

    public DiagnosticDispatchResult emitCleanTeardownFinalSnapshot(
            DiagnosticFinalSnapshotPhysicalEmission emission) {
        Objects.requireNonNull(emission, "emission");
        ReentrantReadWriteLock.WriteLock write = eligibilityLock.writeLock();
        write.lock();
        try {
            boolean eligible = mode.isBoundaryEnabled() && !cleanTeardownStarted;
            cleanTeardownStarted = true;
            DiagnosticSessionAdmissionAuthority current = authority;
            DiagnosticAdmissionDecision admission = current.admitFinalSnapshot(eligible);
            DiagnosticEmissionOutcome outcome = admission.admitted()
                    ? emit(current, admission.token(), () -> emission.emit(admission.snapshot()))
                    : null;
            return new DiagnosticDispatchResult(admission, outcome, null);
        } finally {
            write.unlock();
        }
    }

    public DiagnosticSessionSnapshot snapshot() {
        ReentrantReadWriteLock.ReadLock read = eligibilityLock.readLock();
        read.lock();
        try {
            return authority.snapshot();
        } finally {
            read.unlock();
        }
    }

    public DiagnosticSessionSnapshot replaceOffSessionForTests(
            String freshDiagnosticSessionId) {
        return replaceOffSessionForTestsWithResult(freshDiagnosticSessionId)
                .disposedSessionSnapshot();
    }

    public DiagnosticSessionTestResetResult replaceOffSessionForTestsWithResult(
            String freshDiagnosticSessionId) {
        ReentrantReadWriteLock.WriteLock write = eligibilityLock.writeLock();
        write.lock();
        try {
            if (!mode.isOff()) {
                throw new IllegalStateException(
                        "The lightweight integration reset seam is OFF-only; use the core reset coordinator for an enabled final snapshot."
                );
            }
            DiagnosticSessionTestResetResult result =
                    new DiagnosticSessionTestResetCoordinator().reset(
                            authority,
                            freshDiagnosticSessionId,
                            false,
                            ignored -> {
                                throw new AssertionError("OFF reset must not format a snapshot.");
                            },
                            ignored -> {
                                throw new AssertionError("OFF reset must not emit a snapshot.");
                            }
                    );
            authority = result.freshSession();
            cleanTeardownStarted = false;
            return result;
        } finally {
            write.unlock();
        }
    }

    private static DiagnosticEmissionOutcome emit(
            DiagnosticSessionAdmissionAuthority current,
            DiagnosticAdmissionToken token,
            DiagnosticPhysicalEmission emission) {
        return new DiagnosticEmissionCoordinator(current).emit(
                token,
                emission,
                ignored -> "",
                ignored -> emission.emit()
        );
    }

    private static void notifyAdmissionObserver(
            DiagnosticDispatchObserver observer,
            DiagnosticAdmissionDecision admission) {
        try {
            observer.admissionDecided(admission);
        } catch (RuntimeException | LinkageError ignored) {
            // An observer is diagnostic bookkeeping only; it must not strand shared admission
            // or alter the observed gameplay call path.
        }
    }

    private static void notifySettlementObserver(
            DiagnosticDispatchObserver observer,
            DiagnosticEmissionOutcome outcome) {
        try {
            observer.emissionSettled(outcome);
        } catch (RuntimeException | LinkageError ignored) {
            // Shared settlement is already complete. Observer failure is intentionally isolated.
        }
    }
}
