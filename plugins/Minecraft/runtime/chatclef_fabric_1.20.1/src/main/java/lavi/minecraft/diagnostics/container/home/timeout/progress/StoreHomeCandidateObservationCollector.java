package lavi.minecraft.diagnostics.container.home.timeout.progress;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.container.home.timeout.candidate.StoreHomeCandidateBoundaryDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;

import java.util.Objects;

//20260907_kpopmodder: Collect immutable STORE_HOME candidate observations without owning lifecycle state or emission.
public final class StoreHomeCandidateObservationCollector {
    private final StoreHomeProgressSnapshotReader snapshotReader;
    private final StoreHomeCandidateBoundaryDiagnostics candidateDiagnostics;

    public StoreHomeCandidateObservationCollector(
            StoreHomeProgressSnapshotReader snapshotReader,
            StoreHomeCandidateBoundaryDiagnostics candidateDiagnostics) {
        this.snapshotReader = Objects.requireNonNull(
                snapshotReader, "snapshotReader"
        );
        this.candidateDiagnostics = Objects.requireNonNull(
                candidateDiagnostics, "candidateDiagnostics"
        );
    }

    public StoreHomeProgressSnapshot capture(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session) {
        return snapshotReader.capture(mod, candidate, session);
    }

    public StoreHomePlayerPositionSnapshot capturePlayer(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate) {
        return snapshotReader.capturePlayer(mod, candidate);
    }

    public StoreHomeCandidateProgressObservation startedObservation(
            String eventName,
            long operationId,
            StoreHomePhase phase,
            String reason,
            StoreHomeCandidateProgressState active,
            StoreHomeProgressSnapshot snapshot) {
        return immutableObservation(
                eventName,
                operationId,
                phase,
                reason,
                active,
                snapshot,
                "NO_NEW_PROGRESS",
                true,
                false
        );
    }

    public StoreHomeCandidateProgressObservation boundaryObservation(
            String eventName,
            long operationId,
            StoreHomePhase phase,
            String reason,
            StoreHomeCandidateProgressState active,
            StoreHomeProgressSnapshot snapshot,
            StoreHomeCandidateProgressObservation observed) {
        Objects.requireNonNull(observed, "observed");
        return immutableObservation(
                eventName,
                operationId,
                phase,
                reason,
                active,
                snapshot,
                observed.progressKind(),
                observed.semanticStateChanged(),
                observed.sampleDue()
        );
    }

    public Object[] unavailableEvidence(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateCount,
            int candidateQueueRemaining,
            HomeStorageContainerSession session) {
        return candidateDiagnostics.unavailableEvidence(
                context,
                candidate,
                candidateCount,
                candidateQueueRemaining,
                session
        );
    }

    public Object[] unobservedRejectionEvidence(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateCount,
            int candidateQueueRemaining,
            int candidateTicks) {
        return candidateDiagnostics.unobservedRejectionEvidence(
                context,
                candidate,
                candidateCount,
                candidateQueueRemaining,
                candidateTicks
        );
    }

    public Object[] observedEvidence(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            StoreHomeCandidateProgressState active,
            HomeStorageContainerSession session,
            StoreHomeProgressSnapshot snapshot,
            StoreHomeCandidateProgressObservation observed,
            int candidateQueueRemaining,
            boolean candidateIncludedInQueue,
            int candidateTicks,
            long clientTickId,
            String diagnosticBoundaryKind,
            long operationId,
            StoreHomePhase phase,
            String diagnosticBoundaryReason) {
        StoreHomeCandidateProgressObservation boundaryObservation =
                boundaryObservation(
                        diagnosticBoundaryKind,
                        operationId,
                        phase,
                        diagnosticBoundaryReason,
                        active,
                        snapshot,
                        observed
                );
        return candidateDiagnostics.observedEvidence(
                context,
                candidate,
                active,
                session,
                snapshot,
                boundaryObservation,
                candidateQueueRemaining,
                candidateIncludedInQueue,
                candidateTicks,
                clientTickId,
                diagnosticBoundaryKind,
                diagnosticBoundaryReason
        );
    }

    private static StoreHomeCandidateProgressObservation immutableObservation(
            String eventName,
            long operationId,
            StoreHomePhase phase,
            String reason,
            StoreHomeCandidateProgressState active,
            StoreHomeProgressSnapshot snapshot,
            String progressKind,
            boolean semanticStateChanged,
            boolean sampleDue) {
        Objects.requireNonNull(active, "active");
        Objects.requireNonNull(snapshot, "snapshot");
        String fingerprint = StoreHomeProgressFingerprint.create(
                eventName,
                operationId,
                active.candidateAttemptOrdinal(),
                active.candidate().destinationId(),
                phase,
                progressKind,
                reason,
                active.childTaskClass(),
                active.childTaskRunOrdinal(),
                snapshot
        );
        return new StoreHomeCandidateProgressObservation(
                progressKind,
                fingerprint,
                semanticStateChanged,
                sampleDue
        );
    }
}
