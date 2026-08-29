package lavi.minecraft.task.container.home.execution.session.transfer;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateFailureKind;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateRejector;
import lavi.minecraft.task.container.home.execution.operation.pending.StoreHomePendingOwnershipGuard;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.HomeStorageConfirmedTransferCommit;
import lavi.minecraft.task.container.home.execution.session.HomeStorageConfirmedTransferCommitter;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.transfer.HomeStorageTransferResult;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;

import java.util.Objects;

//20260829_kpopmodder: Apply one transfer result to the owning session and operation state.
public final class StoreHomeTransferResultHandler {
    private final StoreHomeExecutionState state;
    private final HomeStorageTransferExecutor transferExecutor;
    private final HomeStorageConfirmedTransferCommitter transferCommitter;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics;
    private final AutoDepositExactOpenContainerBinding binding;
    private final StoreHomePendingOwnershipGuard pendingGuard;
    private final StoreHomeCandidateRejector candidateRejector;
    private final StoreHomeOperationTerminator terminator;

    public StoreHomeTransferResultHandler(
            StoreHomeExecutionState state,
            HomeStorageTransferExecutor transferExecutor,
            HomeStorageConfirmedTransferCommitter transferCommitter,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics,
            AutoDepositExactOpenContainerBinding binding,
            StoreHomePendingOwnershipGuard pendingGuard,
            StoreHomeCandidateRejector candidateRejector,
            StoreHomeOperationTerminator terminator) {
        this.state = Objects.requireNonNull(state, "state");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
        this.transferCommitter = Objects.requireNonNull(
                transferCommitter, "transferCommitter"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.manifestStaleDiagnostics = Objects.requireNonNull(
                manifestStaleDiagnostics, "manifestStaleDiagnostics"
        );
        this.binding = Objects.requireNonNull(binding, "binding");
        this.pendingGuard = Objects.requireNonNull(pendingGuard, "pendingGuard");
        this.candidateRejector = Objects.requireNonNull(
                candidateRejector, "candidateRejector"
        );
        this.terminator = Objects.requireNonNull(terminator, "terminator");
    }

    public Task handle(
            Task diagnosticOwner,
            AltoClef mod,
            HomeStorageManifestStep step,
            HomeStorageTransferResult transfer) {
        switch (transfer.status()) {
            case WAITING -> {
                return null;
            }
            case CLICK_REQUESTED -> {
                if (!transferExecutor.hasPending()
                        || state.session().current().pendingTransfer().isPresent()) {
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "click_request_pending_ownership_mismatch"
                    );
                    return null;
                }
                state.session().replace(state.session().current().beginTransfer(
                        state.operation().current().operationId(), step
                ));
                return null;
            }
            case CONTAINER_NOT_OPEN -> {
                if (state.session().current().pendingTransfer().isPresent()
                        || transferExecutor.hasPending()) {
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "container_closed_with_pending_transfer"
                    );
                } else {
                    candidateRejector.reject(
                            diagnosticOwner,
                            mod,
                            transfer.reason(),
                            StoreHomeCandidateFailureKind.UNAVAILABLE
                    );
                }
                return null;
            }
            case TRANSFERRED -> {
                if (transferExecutor.hasPending()
                        || state.session().current().pendingTransfer().isEmpty()) {
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "confirmed_transfer_pending_ownership_mismatch"
                    );
                    return null;
                }
                HomeStorageConfirmedTransferCommit commit = transferCommitter.prepare(
                        state.operation().current(),
                        state.session().current(),
                        step,
                        transfer.transferredCount(),
                        transfer.sourceCountAfter()
                );
                // Both immutable values were prepared above; these assignments
                // have no callback or yield between their commits.
                state.session().replace(commit.session());
                state.operation().replace(commit.operation());
                if (commit.newlyCommitted()) {
                    timeoutLifecycle.recordConfirmedTransfer();
                }
                if (state.session().current().progress().complete()) {
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.COMPLETED,
                            transfer.reason()
                    );
                }
                return null;
            }
            case NO_CAPACITY -> {
                if (pendingGuard.hasAnyPending()) {
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "capacity_result_with_pending_transfer"
                    );
                } else {
                    candidateRejector.reject(
                            diagnosticOwner,
                            mod,
                            transfer.reason(),
                            StoreHomeCandidateFailureKind.CAPACITY
                    );
                }
                return null;
            }
            case NO_PROGRESS -> {
                if (pendingGuard.hasAnyPending()) {
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "no_progress_with_pending_transfer"
                    );
                    return null;
                }
                candidateRejector.reject(
                        diagnosticOwner,
                        mod,
                        transfer.reason(),
                        StoreHomeCandidateFailureKind.UNAVAILABLE
                );
                return null;
            }
            case MANIFEST_STALE -> {
                manifestStaleDiagnostics.recordExecutorStale(
                        diagnosticOwner,
                        mod,
                        state.lifecycle().phase(),
                        transfer.reason(),
                        state.session().current().progress(),
                        step,
                        state.session().current().candidate().destination(),
                        binding,
                        transfer.failureObservation()
                );
                terminator.finish(
                        diagnosticOwner,
                        mod,
                        StoreHomeResult.MANIFEST_STALE,
                        transfer.reason()
                );
                return null;
            }
            case CURSOR_NOT_EMPTY -> {
                terminator.finish(
                        diagnosticOwner,
                        mod,
                        StoreHomeResult.CURSOR_NOT_EMPTY,
                        transfer.reason()
                );
                return null;
            }
            case TRANSFER_UNCONFIRMED -> {
                terminator.finish(
                        diagnosticOwner,
                        mod,
                        StoreHomeResult.TRANSFER_UNCONFIRMED,
                        transfer.reason()
                );
                return null;
            }
        }
        throw new IllegalStateException(
                "Unhandled transfer status " + transfer.status()
        );
    }
}
