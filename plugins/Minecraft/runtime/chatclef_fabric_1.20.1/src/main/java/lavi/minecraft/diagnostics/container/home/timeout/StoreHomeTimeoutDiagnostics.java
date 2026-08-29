package lavi.minecraft.diagnostics.container.home.timeout;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.artifact.StoreHomeRunManifestDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeEventFields;
import lavi.minecraft.diagnostics.container.home.timeout.guard.StoreHomeDiagnosticBookkeepingGuard;
import lavi.minecraft.diagnostics.container.home.timeout.guard.StoreHomeDiagnosticBoundary;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressState;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressObservation;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomePlayerPositionSnapshot;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressFingerprint;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressSnapshot;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressSnapshotReader;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticCandidateCatalogState;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticCandidateProgressLifecycle;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticOperationState;
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

//20260828_kpopmodder: Observe STORE_HOME lifecycle and timeout boundaries without changing Task decisions.
public final class StoreHomeTimeoutDiagnostics {
    private static final String OPERATION_STARTED = "STORE_HOME_OPERATION_STARTED";
    private static final String CANDIDATE_STARTED =
            "STORE_HOME_CANDIDATE_ATTEMPT_STARTED";
    private static final String CANDIDATE_PROGRESS =
            "STORE_HOME_CANDIDATE_PROGRESS_SUMMARY";
    private static final String CANDIDATE_TIMEOUT =
            "STORE_HOME_CANDIDATE_TIMEOUT_DECISION";
    private static final String OPERATION_TIMEOUT =
            "STORE_HOME_OPERATION_TIMEOUT_DECISION";
    private static final String CANDIDATE_REJECTED =
            "STORE_HOME_CANDIDATE_REJECTED";
    private static final String CANDIDATE_ACTIVATED =
            "STORE_HOME_CANDIDATE_ACTIVATED";
    private static final String OPERATION_TERMINAL =
            "STORE_HOME_OPERATION_TERMINAL_SUMMARY";

    private final long operationId;
    private final StoreHomeDiagnosticEmitter emitter;
    private final StoreHomeProgressSnapshotReader snapshotReader;
    private final String topLevelTaskRunId;

    private final StoreHomeDiagnosticOperationState operationState;
    private final StoreHomeDiagnosticCandidateCatalogState candidateCatalog =
            new StoreHomeDiagnosticCandidateCatalogState();
    private final StoreHomeDiagnosticCandidateProgressLifecycle candidateProgress =
            new StoreHomeDiagnosticCandidateProgressLifecycle();

    public StoreHomeTimeoutDiagnostics(
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
        this.snapshotReader = new StoreHomeProgressSnapshotReader(
                Objects.requireNonNull(exactBinding, "exactBinding"),
                Objects.requireNonNull(transferExecutor, "transferExecutor")
        );
        this.topLevelTaskRunId = "store-home-operation-" + operationId;
    }

    public void recordOperationStarted(
            Task owner,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout) {
        observeTimeout(timeout);
        StoreHomeDiagnosticBoundary.runIfEnabled(
                () -> ensureOperationStarted(owner, phase, operation)
        );
    }

    public void recordCandidateCatalog(int candidateCount) {
        StoreHomeDiagnosticBookkeepingGuard.runSafely(
                () -> candidateCatalog.captureCatalog(candidateCount)
        );
    }

    public void recordCandidateRejectedBeforeAttempt(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingAfterRejection,
            String reason,
            String failureKind) {
        observeTimeout(timeout);
        int candidateOrdinal = candidateCatalog.reserveCandidateOrdinal(
                remainingAfterRejection + 1
        );
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            long clientTickId = currentClientTickId();
            emitter.emitBoundary(
                    CANDIDATE_REJECTED,
                    reason,
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            StoreHomeEventFields.operationContext(context, candidate),
                            StoreHomeEventFields.candidateBeforeAttempt(
                                    operationId,
                                    candidate,
                                    candidateOrdinal,
                                    candidateCatalog.effectiveCandidateCount(
                                            remainingAfterRejection + 1
                                    ),
                                    remainingAfterRejection
                            ),
                            new Object[]{
                                    "rejectionStage", "PRE_ATTEMPT_VALIDATION",
                                    "candidateAttemptStarted", false,
                                    "candidateActuallyRemoved", true,
                                    "rejectionReason", reason,
                                    "actualRejectionReason", reason,
                                    "failureKind", failureKind,
                                    "actualAction",
                                    remainingAfterRejection > 0
                                            ? "NEXT_CANDIDATE_SELECTION_PENDING"
                                            : "CANDIDATE_QUEUE_EXHAUSTED_PENDING_TERMINAL"
                            }
                    )
            );
        });
    }

    public void recordCandidateStarted(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent) {
        observeTimeout(timeout);
        candidateCatalog.rememberAttempt(
                attempt, remainingCandidateCountIncludingCurrent
        );
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            startCandidate(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    remainingCandidateCountIncludingCurrent,
                    "candidate_attempt_created",
                    candidateCatalog.knownCandidateOrdinal(),
                    candidateCatalog.knownCandidateAttemptOrdinal()
            );
        });
    }

    public void recordProgress(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            Task activeChildTask) {
        observeTimeout(timeout);
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            if (attempt == null) {
                return;
            }
            ensureActiveCandidate(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    remainingCandidateCountIncludingCurrent
            );
            StoreHomeCandidateProgressState active =
                    candidateProgress.activeCandidate();
            long clientTickId = currentClientTickId();
            if (!emitter.canObserveProgress(active.candidateId())) {
                StoreHomePlayerPositionSnapshot player = snapshotReader.capturePlayer(
                        mod, attempt.candidate()
                );
                active.observePlayerOnly(
                        clientTickId, player, activeChildTask
                );
                active.recordSuppressedRepeat();
                emitter.recordProgressSuppressed(CANDIDATE_PROGRESS);
                return;
            }
            StoreHomeProgressSnapshot snapshot = snapshotReader.capture(
                    mod, attempt.candidate(), session
            );
            StoreHomeCandidateProgressObservation observation =
                    active.observe(
                            clientTickId, phase, snapshot, activeChildTask
                    );
            if (!observation.shouldEmit()) {
                active.recordSuppressedRepeat();
                emitter.recordProgressSuppressed(CANDIDATE_PROGRESS);
                return;
            }
            int suppressedBefore = active.suppressedRepeatCount();
            boolean emitted = emitter.emitProgress(
                    CANDIDATE_PROGRESS,
                    observation.progressKind().toLowerCase(),
                    active.candidateId(),
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            StoreHomeEventFields.operationContext(
                                    context, attempt.candidate()
                            ),
                            StoreHomeEventFields.candidate(
                                    active,
                                    remainingCandidateCountIncludingCurrent,
                                    candidateTicks(),
                                    clientTickId
                            ),
                            StoreHomeEventFields.progress(
                                    active,
                                    snapshot,
                                    observation,
                                    clientTickId,
                                    suppressedBefore
                            ),
                            StoreHomeEventFields.session(session)
                    )
            );
            if (emitted) {
                active.markProgressEmitted(
                        clientTickId, observation.fingerprint()
                );
            } else {
                active.recordSuppressedRepeat();
            }
        });
    }

    public void recordOperationTimeoutDecision(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeTimeoutReason timeoutReason,
            boolean pendingAtDecision,
            Task activeChildTask) {
        observeTimeout(timeout);
        String stableReason = timeoutReason == null
                ? "unavailable_timeout_reason"
                : timeoutReason.stableReason();
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            long clientTickId = currentClientTickId();
            Object candidateTicksObserved = attempt == null
                    ? "unavailable_no_active_candidate"
                    : candidateTicks();
            Object[] evidence = candidateEvidence(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    session,
                    remainingCandidateCountIncludingCurrent,
                    activeChildTask,
                    clientTickId,
                    OPERATION_TIMEOUT,
                    stableReason
            );
            Object[] captureStatus = attempt == null
                    ? new Object[]{
                    "diagnosticCaptureStatus", "complete_no_active_candidate",
                    "diagnosticErrorClass", "none"
            }
                    : new Object[0];
            boolean sessionPending = session != null
                    && session.pendingTransfer().isPresent();
            emitter.emitBoundary(
                    OPERATION_TIMEOUT,
                    stableReason,
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            evidence,
                            captureStatus,
                            StoreHomeEventFields.timeoutDecision(
                                    "OPERATION",
                                    clientTickId,
                                    candidateTicksObserved,
                                    operationDecisionTicks(timeoutReason),
                                    false,
                                    "unavailable_not_evaluated",
                                    true,
                                    true,
                                    pendingAtDecision
                                            ? "FINISH_TRANSFER_UNCONFIRMED"
                                            : "FINISH_EXHAUSTED",
                                    "not_applicable",
                                    pendingAtDecision
                                            ? stableReason + "_with_pending_transfer"
                                            : stableReason,
                                    pendingAtDecision
                                            ? "TRANSFER_UNCONFIRMED"
                                            : "EXISTING_EXHAUSTED_CLASSIFIER",
                                    pendingAtDecision,
                                    sessionPending
                            ),
                            new Object[]{
                                    "candidateTicksObservedAtOperationDecision",
                                    candidateTicksObserved,
                                    "operationDecisionCounterKind",
                                    operationDecisionCounterKind(timeoutReason),
                                    "operationDecisionCounterLimitTicks",
                                    operationDecisionCounterLimit(timeoutReason)
                            }
                    )
            );
        });
    }

    public void recordCandidateTimeoutDecision(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeTimeoutReason timeoutReason,
            boolean pendingAtDecision,
            Task activeChildTask) {
        observeTimeout(timeout);
        String stableReason = timeoutReason == null
                ? "unavailable_timeout_reason"
                : timeoutReason.stableReason();
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            ensureActiveCandidate(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    remainingCandidateCountIncludingCurrent
            );
            long clientTickId = currentClientTickId();
            boolean sessionPending = session != null
                    && session.pendingTransfer().isPresent();
            Object[] evidence = candidateEvidence(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    session,
                    remainingCandidateCountIncludingCurrent,
                    activeChildTask,
                    clientTickId,
                    CANDIDATE_TIMEOUT,
                    stableReason
            );
            emitter.emitBoundary(
                    CANDIDATE_TIMEOUT,
                    stableReason,
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            evidence,
                            StoreHomeEventFields.timeoutDecision(
                                    "CANDIDATE",
                                    clientTickId,
                                    candidateTicks(),
                                    operationTicks(),
                                    true,
                                    true,
                                    true,
                                    false,
                                    pendingAtDecision
                                            ? "FINISH_TRANSFER_UNCONFIRMED"
                                            : "REJECT_CANDIDATE",
                                    pendingAtDecision
                                            ? "not_applicable"
                                            : stableReason,
                                    pendingAtDecision
                                            ? stableReason + "_with_pending_transfer"
                                            : "not_applicable",
                                    pendingAtDecision
                                            ? "TRANSFER_UNCONFIRMED"
                                            : "not_applicable",
                                    pendingAtDecision,
                                    sessionPending
                            ),
                            new Object[]{
                                    "candidateDecisionCounterKind",
                                    candidateDecisionCounterKind(timeoutReason),
                                    "candidateDecisionCounterLimitTicks",
                                    operationState.observation()
                                            .activeCandidateLimitTicks()
                            }
                    )
            );
        });
    }

    public void recordCandidateRejected(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session,
            int candidateActiveTicksAtRejection,
            int remainingCandidateCountAfterRejection,
            String reason,
            String failureKind,
            Task activeChildTask) {
        observeTimeout(timeout);
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            StoreHomeCandidateProgressState active =
                    candidateProgress.activeCandidate();
            if (active != null && active.candidate() != candidate) {
                candidateProgress.clearActive();
                active = null;
            }
            if (active == null && candidateCatalog.knownCandidateMatches(candidate)) {
                attachCandidateState(
                        mod,
                        phase,
                        candidateCatalog.knownAttemptReference(),
                        remainingCandidateCountAfterRejection + 1,
                        candidateCatalog.knownCandidateOrdinal(),
                        candidateCatalog.knownCandidateAttemptOrdinal()
                );
                active = candidateProgress.activeCandidate();
            }
            long clientTickId = currentClientTickId();
            Object[] evidence = active == null
                    ? StoreHomeDiagnosticEmitter.merge(
                    StoreHomeEventFields.operationContext(context, candidate),
                    StoreHomeEventFields.candidateAfterUnobservedAttempt(
                            candidate,
                            candidateCatalog.effectiveCandidateCount(
                                    remainingCandidateCountAfterRejection + 1
                            ),
                            remainingCandidateCountAfterRejection,
                            candidateTicks()
                    )
            )
                    : candidateEvidenceForKnownState(
                    mod,
                    phase,
                    context,
                    candidate,
                    session,
                    candidateTicks(),
                    remainingCandidateCountAfterRejection,
                    false,
                    activeChildTask,
                    clientTickId,
                    CANDIDATE_REJECTED,
                    reason
            );
            emitter.emitBoundary(
                    CANDIDATE_REJECTED,
                    reason,
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            evidence,
                            new Object[]{
                                    "rejectionStage", "ACTIVE_ATTEMPT",
                                    "candidateAttemptStarted", true,
                                    "candidateActuallyRemoved", true,
                                    "rejectionReason", reason,
                                    "actualRejectionReason", reason,
                                    "failureKind", failureKind,
                                    "remainingCandidateCountAfterRejection",
                                    remainingCandidateCountAfterRejection,
                                    "candidateActiveTicksAtRejection",
                                    candidateActiveTicksAtRejection,
                                    "actualAction",
                                    remainingCandidateCountAfterRejection > 0
                                            ? "NEXT_CANDIDATE_SELECTION_PENDING"
                                            : "CANDIDATE_QUEUE_EXHAUSTED_PENDING_TERMINAL"
                            }
                    )
            );
            candidateProgress.clearActive();
        });
        candidateProgress.clearActive();
        candidateCatalog.clearKnownAttempt();
    }

    public void recordCandidateActivated(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent) {
        observeTimeout(timeout);
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            ensureActiveCandidate(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    remainingCandidateCountIncludingCurrent
            );
            long clientTickId = currentClientTickId();
            Object[] evidence = candidateEvidenceForKnownState(
                    mod,
                    phase,
                    context,
                    attempt.candidate(),
                    session,
                    candidateTicks(),
                    remainingCandidateCountIncludingCurrent,
                    true,
                    null,
                    clientTickId,
                    CANDIDATE_ACTIVATED,
                    "exact_trusted_container_session_activated"
            );
            emitter.emitBoundary(
                    CANDIDATE_ACTIVATED,
                    "exact_trusted_container_session_activated",
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            evidence,
                            new Object[]{
                                    "candidateActivated", true,
                                    "activationResult", "EXACT_SESSION_INSTALLED"
                            }
                    )
            );
        });
    }

    public void recordTerminal(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeResult result,
            String reason) {
        observeTimeout(timeout);
        StoreHomeDiagnosticBoundary.runIfEnabled(() -> {
            ensureOperationStarted(owner, phase, operation);
            long clientTickId = currentClientTickId();
            Object[] evidence = attempt == null
                    ? StoreHomeDiagnosticEmitter.merge(
                    StoreHomeEventFields.operationContext(context, null),
                    StoreHomeEventFields.candidateUnavailable(
                            candidateCatalog.effectiveCandidateCount(
                                    remainingCandidateCountIncludingCurrent
                            ),
                            remainingCandidateCountIncludingCurrent
                    ),
                    StoreHomeEventFields.session(session)
            )
                    : candidateEvidence(
                    owner,
                    mod,
                    phase,
                    operation,
                    context,
                    attempt,
                    session,
                    remainingCandidateCountIncludingCurrent,
                    session == null ? attempt.currentOpenTask() : null,
                    clientTickId,
                    OPERATION_TERMINAL,
                    reason
            );
            emitter.emitTerminal(
                    OPERATION_TERMINAL,
                    reason,
                    owner,
                    StoreHomeDiagnosticEmitter.merge(
                            operationFields(owner, phase, clientTickId, operation),
                            evidence,
                            StoreHomeEventFields.terminal(result, reason, operation),
                            new Object[]{
                                    "budgetSummaryCapturedBeforeTerminalEmission", true
                            },
                            emitter.budgetSummaryFields()
                    )
            );
            candidateProgress.clearActive();
        });
    }

    private void startCandidate(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent,
            String reason,
            int candidateOrdinal,
            int candidateAttemptOrdinal) {
        long clientTickId = currentClientTickId();
        StoreHomeProgressSnapshot snapshot = attachCandidateState(
                mod,
                phase,
                attempt,
                remainingCandidateCountIncludingCurrent,
                candidateOrdinal,
                candidateAttemptOrdinal
        );
        StoreHomeCandidateProgressState active =
                candidateProgress.activeCandidate();
        String fingerprint = StoreHomeProgressFingerprint.create(
                CANDIDATE_STARTED,
                operationId,
                active.candidateAttemptOrdinal(),
                attempt.candidate().destinationId(),
                phase,
                "NO_NEW_PROGRESS",
                reason,
                active.childTaskClass(),
                active.childTaskRunOrdinal(),
                snapshot
        );
        StoreHomeCandidateProgressObservation startObservation =
                new StoreHomeCandidateProgressObservation(
                        "NO_NEW_PROGRESS", fingerprint, true, false
                );
        emitter.emitBoundary(
                CANDIDATE_STARTED,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields(owner, phase, clientTickId, operation),
                        StoreHomeEventFields.operationContext(
                                context, attempt.candidate()
                        ),
                        StoreHomeEventFields.candidate(
                                active,
                                remainingCandidateCountIncludingCurrent,
                                candidateTicks(),
                                clientTickId
                        ),
                        StoreHomeEventFields.progress(
                                active,
                                snapshot,
                                startObservation,
                                clientTickId,
                                0
                        ),
                        new Object[]{
                                "diagnosticBoundaryKind", CANDIDATE_STARTED
                        },
                        StoreHomeEventFields.session(null)
                )
        );
    }

    private StoreHomeProgressSnapshot attachCandidateState(
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent,
            int candidateOrdinal,
            int candidateAttemptOrdinal) {
        long clientTickId = currentClientTickId();
        StoreHomeProgressSnapshot snapshot = snapshotReader.capture(
                mod, attempt.candidate(), null
        );
        StoreHomeCandidateProgressState active =
                new StoreHomeCandidateProgressState(
                operationId,
                attempt.candidate(),
                candidateOrdinal,
                candidateAttemptOrdinal,
                candidateCatalog.effectiveCandidateCount(
                        remainingCandidateCountIncludingCurrent
                ),
                clientTickId,
                operationState.observation().candidateActiveTicks() == 0,
                phase,
                snapshot,
                attempt.currentOpenTask()
        );
        candidateProgress.activate(active, attempt);
        return snapshot;
    }

    private void ensureActiveCandidate(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
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
                owner,
                mod,
                phase,
                operation,
                context,
                attempt,
                remainingCandidateCountIncludingCurrent,
                "candidate_attempt_observed_after_diagnostics_attach",
                candidateCatalog.knownCandidateOrdinal(),
                candidateCatalog.knownCandidateAttemptOrdinal()
        );
    }

    private Object[] candidateEvidence(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            Task activeChildTask,
            long clientTickId,
            String diagnosticBoundaryKind,
            String diagnosticBoundaryReason) {
        if (attempt == null) {
            return StoreHomeDiagnosticEmitter.merge(
                    StoreHomeEventFields.operationContext(context, null),
                    StoreHomeEventFields.candidateUnavailable(
                            candidateCatalog.effectiveCandidateCount(
                                    remainingCandidateCountIncludingCurrent
                            ),
                            remainingCandidateCountIncludingCurrent
                    ),
                    StoreHomeEventFields.session(session)
            );
        }
        ensureActiveCandidate(
                owner,
                mod,
                phase,
                operation,
                context,
                attempt,
                remainingCandidateCountIncludingCurrent
        );
        return candidateEvidenceForKnownState(
                mod,
                phase,
                context,
                attempt.candidate(),
                session,
                candidateTicks(),
                remainingCandidateCountIncludingCurrent,
                true,
                activeChildTask,
                clientTickId,
                diagnosticBoundaryKind,
                diagnosticBoundaryReason
        );
    }

    private Object[] candidateEvidenceForKnownState(
            AltoClef mod,
            StoreHomePhase phase,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session,
            int candidateTicks,
            int candidateQueueRemaining,
            boolean candidateIncludedInQueue,
            Task activeChildTask,
            long clientTickId,
            String diagnosticBoundaryKind,
            String diagnosticBoundaryReason) {
        StoreHomeProgressSnapshot snapshot = snapshotReader.capture(
                mod, candidate, session
        );
        StoreHomeCandidateProgressState active =
                candidateProgress.activeCandidate();
        StoreHomeCandidateProgressObservation observed =
                active.observe(
                        clientTickId, phase, snapshot, activeChildTask
                );
        String fingerprint = StoreHomeProgressFingerprint.create(
                diagnosticBoundaryKind,
                operationId,
                active.candidateAttemptOrdinal(),
                candidate.destinationId(),
                phase,
                observed.progressKind(),
                diagnosticBoundaryReason,
                active.childTaskClass(),
                active.childTaskRunOrdinal(),
                snapshot
        );
        StoreHomeCandidateProgressObservation boundaryObservation =
                new StoreHomeCandidateProgressObservation(
                        observed.progressKind(),
                        fingerprint,
                        observed.semanticStateChanged(),
                        observed.sampleDue()
                );
        Object[] candidateFields = candidateIncludedInQueue
                ? StoreHomeEventFields.candidate(
                        active,
                        candidateQueueRemaining,
                        candidateTicks,
                        clientTickId
                )
                : StoreHomeEventFields.candidateAfterRejection(
                        active,
                        candidateQueueRemaining,
                        candidateTicks,
                        clientTickId
                );
        return StoreHomeDiagnosticEmitter.merge(
                StoreHomeEventFields.operationContext(context, candidate),
                candidateFields,
                StoreHomeEventFields.progress(
                        active,
                        snapshot,
                        boundaryObservation,
                        clientTickId,
                        active.suppressedRepeatCount()
                ),
                new Object[]{
                        "diagnosticBoundaryKind", diagnosticBoundaryKind,
                        "diagnosticBoundaryReason", diagnosticBoundaryReason
                },
                StoreHomeEventFields.session(session)
        );
    }

    private void ensureOperationStarted(
            Task owner,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation) {
        if (operationState.started()) {
            return;
        }
        operationState.markStarted(currentClientTickId());
        StoreHomeRunManifestDiagnostics.emitOnce(owner, emitter);
        emitter.emitBoundary(
                OPERATION_STARTED,
                operationState.startClientTickKnown()
                        ? "store_home_task_started"
                        : "store_home_diagnostics_attached_after_start",
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields(
                                owner,
                                phase,
                                operationState.startClientTickId(),
                                operation
                        ),
                        StoreHomeEventFields.operationContext(null, null),
                        StoreHomeEventFields.candidateUnavailable(
                                candidateCatalog.catalogCaptured()
                                        ? candidateCatalog.totalCandidateCount()
                                        : "unavailable_not_built",
                                candidateCatalog.catalogCaptured()
                                        ? candidateCatalog.totalCandidateCount()
                                        : "unavailable_not_built"
                        ),
                        new Object[]{
                                "operationStartObservation",
                                operationState.startClientTickKnown()
                                        ? "TASK_ON_START"
                                        : "FIRST_VISIBLE_DIAGNOSTIC_BOUNDARY",
                                "candidateCatalogCaptured",
                                candidateCatalog.catalogCaptured()
                        }
                )
        );
    }

    private Object[] operationFields(
            Task owner,
            StoreHomePhase phase,
            long clientTickId,
            StoreHomeOperationProgress operation) {
        return StoreHomeEventFields.operation(
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

    private String operationDecisionCounterKind(
            StoreHomeTimeoutReason reason) {
        return reason == StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP
                ? "OPERATION_ACTIVE_TICKS"
                : "OPERATION_NO_PROGRESS_TICKS";
    }

    private int operationDecisionCounterLimit(StoreHomeTimeoutReason reason) {
        StoreHomeTimeoutObservation observation = operationState.observation();
        return reason == StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP
                ? observation.maxOperationEmergencyHardCapTicks()
                : observation.maxOperationNoProgressTicks();
    }

    private static String candidateDecisionCounterKind(
            StoreHomeTimeoutReason reason) {
        return reason == StoreHomeTimeoutReason.CANDIDATE_LOCAL_INTERACTION_TIMEOUT
                ? "CANDIDATE_LOCAL_INTERACTION_TICKS"
                : "CANDIDATE_NAVIGATION_NO_PROGRESS_TICKS";
    }

    private static long currentClientTickId() {
        return ChatClefDiagnostics.currentClientTickId();
    }

}
