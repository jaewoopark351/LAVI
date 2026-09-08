package lavi.minecraft.diagnostics.container.home.timeout.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.artifact.StoreHomeRunManifestDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.candidate.StoreHomeCandidateBoundaryDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateObservationCollector;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressObservation;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressState;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomePlayerPositionSnapshot;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressSnapshot;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressSnapshotReader;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticCandidateCatalogState;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticCandidateProgressLifecycle;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticOperationState;
import lavi.minecraft.diagnostics.container.home.timeout.terminal.StoreHomeTerminalSummaryEmitter;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

import java.util.Objects;

//20260907_kpopmodder: Own one STORE_HOME diagnostic lifecycle aggregate and preserve its transition order.
public final class StoreHomeTimeoutLifecycleCoordinator {
    private static final String OPERATION_STARTED = "STORE_HOME_OPERATION_STARTED";
    private static final String CANDIDATE_STARTED = "STORE_HOME_CANDIDATE_ATTEMPT_STARTED";
    private static final String CANDIDATE_PROGRESS = "STORE_HOME_CANDIDATE_PROGRESS_SUMMARY";
    private static final String CANDIDATE_TIMEOUT = "STORE_HOME_CANDIDATE_TIMEOUT_DECISION";
    private static final String OPERATION_TIMEOUT = "STORE_HOME_OPERATION_TIMEOUT_DECISION";
    private static final String CANDIDATE_REJECTED = "STORE_HOME_CANDIDATE_REJECTED";
    private static final String CANDIDATE_ACTIVATED = "STORE_HOME_CANDIDATE_ACTIVATED";
    private static final String OPERATION_TERMINAL = "STORE_HOME_OPERATION_TERMINAL_SUMMARY";

    private final long operationId;
    private final String topLevelTaskRunId;
    private final StoreHomeDiagnosticEmitter emitter;
    private final StoreHomeDiagnosticOperationState operationState;
    private final StoreHomeDiagnosticCandidateCatalogState candidateCatalog = new StoreHomeDiagnosticCandidateCatalogState();
    private final StoreHomeDiagnosticCandidateProgressLifecycle candidateProgress = new StoreHomeDiagnosticCandidateProgressLifecycle();
    private final StoreHomeCandidateBoundaryDiagnostics candidateBoundaryDiagnostics = new StoreHomeCandidateBoundaryDiagnostics();
    private final StoreHomeCandidateProgressDiagnostics progressDiagnostics = new StoreHomeCandidateProgressDiagnostics();
    private final StoreHomeOperationDiagnostics operationDiagnostics = new StoreHomeOperationDiagnostics();
    private final StoreHomeTerminalSummaryEmitter terminalSummaryEmitter = new StoreHomeTerminalSummaryEmitter();
    private final StoreHomeCandidateObservationCollector observationCollector;

    public StoreHomeTimeoutLifecycleCoordinator(
            long operationId,
            StoreHomeTimeoutPolicy timeoutPolicy,
            AutoDepositExactOpenContainerBinding exactBinding,
            HomeStorageTransferExecutor transferExecutor) {
        this.operationId = operationId;
        this.operationState = new StoreHomeDiagnosticOperationState(
                Objects.requireNonNull(timeoutPolicy, "timeoutPolicy")
        );
        this.emitter = new StoreHomeDiagnosticEmitter(
                operationId,
                StoreHomeRunManifestDiagnostics::currentRunManifestId
        );
        this.observationCollector = new StoreHomeCandidateObservationCollector(
                new StoreHomeProgressSnapshotReader(
                        Objects.requireNonNull(exactBinding, "exactBinding"),
                        Objects.requireNonNull(transferExecutor, "transferExecutor")
                ),
                candidateBoundaryDiagnostics
        );
        this.topLevelTaskRunId = "store-home-operation-" + operationId;
    }

    public void recordOperationStarted(
            Task owner, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
    }

    public void recordCandidateCatalog(int candidateCount) {
        candidateCatalog.captureCatalog(candidateCount);
    }

    public void recordCandidateRejectedBeforeAttempt(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingAfterRejection, String reason, String failureKind) {
        observeTimeout(timeout);
        int candidateOrdinal = candidateCatalog.reserveCandidateOrdinal(
                remainingAfterRejection + 1
        );
        ensureOperationStarted(owner, phase);
        long clientTickId = currentClientTickId();
        candidateBoundaryDiagnostics.emitRejectedBeforeAttempt(
                emitter, CANDIDATE_REJECTED, reason, owner,
                operationFields(owner, phase, clientTickId),
                operationId, context, candidate, candidateOrdinal,
                candidateCatalog.effectiveCandidateCount(
                        remainingAfterRejection + 1
                ),
                remainingAfterRejection, failureKind
        );
    }

    public void recordCandidateStarted(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent) {
        observeTimeout(timeout);
        candidateCatalog.rememberAttempt(attempt, remainingCandidateCountIncludingCurrent);
        ensureOperationStarted(owner, phase);
        startCandidate(
                owner, mod, phase, context, attempt,
                remainingCandidateCountIncludingCurrent,
                "candidate_attempt_created",
                candidateCatalog.knownCandidateOrdinal(),
                candidateCatalog.knownCandidateAttemptOrdinal()
        );
    }

    public void recordProgress(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            Task activeChildTask) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
        if (attempt == null) {
            return;
        }
        ensureActiveCandidate(
                owner, mod, phase, context, attempt,
                remainingCandidateCountIncludingCurrent
        );
        StoreHomeCandidateProgressState active = candidateProgress.activeCandidate();
        long clientTickId = currentClientTickId();
        if (!progressDiagnostics.admitsSnapshotCapture(emitter, active.candidateId())) {
            StoreHomePlayerPositionSnapshot player = observationCollector.capturePlayer(
                    mod, attempt.candidate()
            );
            active.observePlayerOnly(clientTickId, player, activeChildTask);
            active.recordSuppressedRepeat();
            progressDiagnostics.recordSuppressed(emitter, CANDIDATE_PROGRESS);
            return;
        }
        StoreHomeProgressSnapshot snapshot = observationCollector.capture(
                mod, attempt.candidate(), session
        );
        StoreHomeCandidateProgressObservation observation = active.observe(
                clientTickId, phase, snapshot, activeChildTask
        );
        if (!progressDiagnostics.admitsSemanticEmission(observation)) {
            active.recordSuppressedRepeat();
            progressDiagnostics.recordSuppressed(emitter, CANDIDATE_PROGRESS);
            return;
        }
        int suppressedBefore = active.suppressedRepeatCount();
        boolean emitted = progressDiagnostics.emitProgress(
                emitter, CANDIDATE_PROGRESS,
                observation.progressKind().toLowerCase(),
                active.candidateId(), owner,
                operationFields(owner, phase, clientTickId),
                context, attempt.candidate(), active,
                remainingCandidateCountIncludingCurrent,
                candidateTicks(), clientTickId,
                snapshot, observation, suppressedBefore, session
        );
        if (emitted) {
            active.markProgressEmitted(clientTickId, observation.fingerprint());
        } else {
            active.recordSuppressedRepeat();
        }
    }

    public void recordOperationTimeoutDecision(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeTimeoutReason timeoutReason, String stableReason,
            boolean pendingAtDecision, Task activeChildTask) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
        long clientTickId = currentClientTickId();
        Object candidateTicksObserved = attempt == null
                ? "unavailable_no_active_candidate"
                : candidateTicks();
        Object[] evidence = candidateEvidence(
                owner, mod, phase, context, attempt, session,
                remainingCandidateCountIncludingCurrent,
                activeChildTask, clientTickId,
                OPERATION_TIMEOUT, stableReason
        );
        boolean sessionPending = session != null
                && session.pendingTransfer().isPresent();
        operationDiagnostics.emitTimeoutDecision(
                emitter, OPERATION_TIMEOUT, stableReason, owner,
                operationFields(owner, phase, clientTickId),
                evidence, attempt != null, clientTickId,
                candidateTicksObserved,
                operationDecisionTicks(timeoutReason),
                timeoutReason, operationState.observation(),
                pendingAtDecision, sessionPending
        );
    }

    public void recordCandidateTimeoutDecision(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeTimeoutReason timeoutReason, String stableReason,
            boolean pendingAtDecision, Task activeChildTask) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
        ensureActiveCandidate(
                owner, mod, phase, context, attempt,
                remainingCandidateCountIncludingCurrent
        );
        long clientTickId = currentClientTickId();
        boolean sessionPending = session != null
                && session.pendingTransfer().isPresent();
        Object[] evidence = candidateEvidence(
                owner, mod, phase, context, attempt, session,
                remainingCandidateCountIncludingCurrent,
                activeChildTask, clientTickId,
                CANDIDATE_TIMEOUT, stableReason
        );
        candidateBoundaryDiagnostics.emitTimeoutDecision(
                emitter, CANDIDATE_TIMEOUT, stableReason, owner,
                operationFields(owner, phase, clientTickId),
                evidence, clientTickId,
                candidateTicks(), operationTicks(), timeoutReason,
                operationState.observation(),
                pendingAtDecision, sessionPending
        );
    }

    public void recordCandidateRejected(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session,
            int candidateActiveTicksAtRejection, int remainingCandidateCountAfterRejection,
            String reason, String failureKind, Task activeChildTask) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
        StoreHomeCandidateProgressState active = candidateProgress.activeCandidate();
        if (active != null && active.candidate() != candidate) {
            candidateProgress.clearActive();
            active = null;
        }
        if (active == null && candidateCatalog.knownCandidateMatches(candidate)) {
            attachCandidateState(
                    mod, phase,
                    candidateCatalog.knownAttemptReference(),
                    remainingCandidateCountAfterRejection + 1,
                    candidateCatalog.knownCandidateOrdinal(),
                    candidateCatalog.knownCandidateAttemptOrdinal()
            );
            active = candidateProgress.activeCandidate();
        }
        long clientTickId = currentClientTickId();
        Object[] evidence = active == null
                ? observationCollector.unobservedRejectionEvidence(
                        context,
                        candidate,
                        candidateCatalog.effectiveCandidateCount(
                                remainingCandidateCountAfterRejection + 1
                        ),
                        remainingCandidateCountAfterRejection,
                        candidateTicks()
                )
                : candidateEvidenceForKnownState(
                        mod, phase, context, candidate, session,
                        candidateTicks(),
                        remainingCandidateCountAfterRejection,
                        false, activeChildTask, clientTickId,
                        CANDIDATE_REJECTED, reason
                );
        candidateBoundaryDiagnostics.emitRejected(
                emitter, CANDIDATE_REJECTED, reason, owner,
                operationFields(owner, phase, clientTickId),
                evidence,
                candidateActiveTicksAtRejection,
                remainingCandidateCountAfterRejection,
                failureKind
        );
        candidateProgress.clearActive();
        candidateCatalog.clearKnownAttempt();
    }

    public void recordCandidateActivated(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
        ensureActiveCandidate(
                owner, mod, phase, context, attempt,
                remainingCandidateCountIncludingCurrent
        );
        long clientTickId = currentClientTickId();
        Object[] evidence = candidateEvidenceForKnownState(
                mod, phase, context, attempt.candidate(), session,
                candidateTicks(),
                remainingCandidateCountIncludingCurrent,
                true, null, clientTickId, CANDIDATE_ACTIVATED,
                "exact_trusted_container_session_activated"
        );
        candidateBoundaryDiagnostics.emitActivated(
                emitter, CANDIDATE_ACTIVATED,
                "exact_trusted_container_session_activated",
                owner, operationFields(owner, phase, clientTickId), evidence
        );
    }

    public void recordTerminal(
            Task owner, AltoClef mod, StoreHomePhase phase,
            StoreHomeOperationProgress operation, StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeResult result, String reason) {
        observeTimeout(timeout);
        ensureOperationStarted(owner, phase);
        long clientTickId = currentClientTickId();
        Object[] evidence = attempt == null
                ? observationCollector.unavailableEvidence(
                        context,
                        null,
                        candidateCatalog.effectiveCandidateCount(
                                remainingCandidateCountIncludingCurrent
                        ),
                        remainingCandidateCountIncludingCurrent,
                        session
                )
                : candidateEvidence(
                        owner, mod, phase, context, attempt, session,
                        remainingCandidateCountIncludingCurrent,
                        session == null ? attempt.currentOpenTask() : null,
                        clientTickId, OPERATION_TERMINAL, reason
                );
        terminalSummaryEmitter.emit(
                emitter, OPERATION_TERMINAL, reason, owner,
                operationFields(owner, phase, clientTickId),
                evidence, result, operation
        );
        candidateProgress.clearActive();
    }

    private void startCandidate(
            Task owner, AltoClef mod, StoreHomePhase phase,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent,
            String reason, int candidateOrdinal, int candidateAttemptOrdinal) {
        long clientTickId = currentClientTickId();
        StoreHomeProgressSnapshot snapshot = attachCandidateState(
                mod, phase, attempt,
                remainingCandidateCountIncludingCurrent,
                candidateOrdinal, candidateAttemptOrdinal
        );
        StoreHomeCandidateProgressState active = candidateProgress.activeCandidate();
        StoreHomeCandidateProgressObservation startObservation =
                observationCollector.startedObservation(
                        CANDIDATE_STARTED, operationId, phase, reason,
                        active, snapshot
                );
        candidateBoundaryDiagnostics.emitStarted(
                emitter, CANDIDATE_STARTED, reason, owner,
                operationFields(owner, phase, clientTickId),
                context, attempt.candidate(), active,
                remainingCandidateCountIncludingCurrent,
                candidateTicks(), clientTickId, snapshot, startObservation
        );
    }

    private StoreHomeProgressSnapshot attachCandidateState(
            AltoClef mod, StoreHomePhase phase, StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent,
            int candidateOrdinal, int candidateAttemptOrdinal) {
        long clientTickId = currentClientTickId();
        StoreHomeProgressSnapshot snapshot = observationCollector.capture(
                mod, attempt.candidate(), null
        );
        StoreHomeCandidateProgressState active =
                new StoreHomeCandidateProgressState(
                        operationId, attempt.candidate(),
                        candidateOrdinal, candidateAttemptOrdinal,
                        candidateCatalog.effectiveCandidateCount(
                                remainingCandidateCountIncludingCurrent
                        ),
                        clientTickId,
                        operationState.observation().candidateActiveTicks() == 0,
                        phase, snapshot, attempt.currentOpenTask()
                );
        candidateProgress.activate(active, attempt);
        return snapshot;
    }

    private void ensureActiveCandidate(
            Task owner, AltoClef mod, StoreHomePhase phase,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent) {
        if (attempt == null) {
            return;
        }
        if (candidateProgress.activeMatches(attempt)) {
            return;
        }
        if (candidateCatalog.knownAttemptReference() != attempt) {
            candidateCatalog.rememberAttempt(
                    attempt, remainingCandidateCountIncludingCurrent
            );
        }
        startCandidate(
                owner, mod, phase, context, attempt,
                remainingCandidateCountIncludingCurrent,
                "candidate_attempt_observed_after_diagnostics_attach",
                candidateCatalog.knownCandidateOrdinal(),
                candidateCatalog.knownCandidateAttemptOrdinal()
        );
    }

    private Object[] candidateEvidence(
            Task owner, AltoClef mod, StoreHomePhase phase,
            HomeStorageOperationContext context, StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            Task activeChildTask, long clientTickId,
            String diagnosticBoundaryKind, String diagnosticBoundaryReason) {
        if (attempt == null) {
            return observationCollector.unavailableEvidence(
                    context,
                    null,
                    candidateCatalog.effectiveCandidateCount(
                            remainingCandidateCountIncludingCurrent
                    ),
                    remainingCandidateCountIncludingCurrent,
                    session
            );
        }
        ensureActiveCandidate(
                owner, mod, phase, context, attempt,
                remainingCandidateCountIncludingCurrent
        );
        return candidateEvidenceForKnownState(
                mod, phase, context, attempt.candidate(), session,
                candidateTicks(),
                remainingCandidateCountIncludingCurrent,
                true, activeChildTask, clientTickId,
                diagnosticBoundaryKind, diagnosticBoundaryReason
        );
    }

    private Object[] candidateEvidenceForKnownState(
            AltoClef mod, StoreHomePhase phase, HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session,
            int candidateTicks, int candidateQueueRemaining,
            boolean candidateIncludedInQueue,
            Task activeChildTask, long clientTickId,
            String diagnosticBoundaryKind, String diagnosticBoundaryReason) {
        StoreHomeProgressSnapshot snapshot = observationCollector.capture(
                mod, candidate, session
        );
        StoreHomeCandidateProgressState active = candidateProgress.activeCandidate();
        StoreHomeCandidateProgressObservation observed = active.observe(
                clientTickId, phase, snapshot, activeChildTask
        );
        return observationCollector.observedEvidence(
                context, candidate, active, session, snapshot, observed,
                candidateQueueRemaining,
                candidateIncludedInQueue,
                candidateTicks, clientTickId,
                diagnosticBoundaryKind, operationId, phase,
                diagnosticBoundaryReason
        );
    }

    private void ensureOperationStarted(
            Task owner,
            StoreHomePhase phase) {
        if (operationState.started()) {
            return;
        }
        operationState.markStarted(currentClientTickId());
        StoreHomeRunManifestDiagnostics.emitOnce(owner, emitter);
        boolean operationStartClientTickKnown =
                operationState.startClientTickKnown();
        Object[] startedOperationFields = operationFields(
                owner,
                phase,
                operationState.startClientTickId()
        );
        boolean candidateCatalogCaptured = candidateCatalog.catalogCaptured();
        operationDiagnostics.emitStarted(
                emitter,
                OPERATION_STARTED,
                owner,
                startedOperationFields,
                operationStartClientTickKnown,
                candidateCatalogCaptured,
                candidateCatalogCaptured
                        ? candidateCatalog.totalCandidateCount()
                        : "unavailable_not_built"
        );
    }

    private Object[] operationFields(
            Task owner,
            StoreHomePhase phase,
            long clientTickId) {
        return operationDiagnostics.operationFields(
                owner,
                topLevelTaskRunId,
                phase,
                clientTickId,
                operationState.startClientTickId(),
                operationState.startClientTickKnown(),
                operationState.observation()
        );
    }

    private void observeTimeout(StoreHomeTimeoutObservation timeout) {
        operationState.observe(timeout);
    }

    private int operationTicks() {
        return operationState.timeoutTicks();
    }

    private int candidateTicks() {
        return operationState.observation().activeCandidateTimeoutTicks();
    }

    private int operationDecisionTicks(StoreHomeTimeoutReason reason) {
        return operationState.decisionTicks(reason);
    }

    private static long currentClientTickId() {
        return ChatClefDiagnostics.currentClientTickId();
    }
}
