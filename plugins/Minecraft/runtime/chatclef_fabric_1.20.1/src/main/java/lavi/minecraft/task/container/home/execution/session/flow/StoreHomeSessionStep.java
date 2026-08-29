package lavi.minecraft.task.container.home.execution.session.flow;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateFailureKind;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateRejector;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivation;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivationGate;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidation;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidator;
import lavi.minecraft.task.container.home.execution.session.transfer.StoreHomeTransferResultHandler;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Own the ordered validation and transfer step of one active session.
public final class StoreHomeSessionStep {
    private final StoreHomeExecutionState state;
    private final HomeStorageContainerActivationGate activationGate;
    private final HomeStorageInventorySnapshotReader snapshotReader;
    private final HomeStorageManifestValidator manifestValidator;
    private final StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics;
    private final HomeStorageScreenSlotResolver slotResolver;
    private final HomeStorageTransferExecutor transferExecutor;
    private final AutoDepositExactOpenContainerBinding binding;
    private final StoreHomeCandidateRejector candidateRejector;
    private final StoreHomeOperationTerminator terminator;
    private final StoreHomeTransferResultHandler transferResultHandler;

    public StoreHomeSessionStep(
            StoreHomeExecutionState state,
            HomeStorageContainerActivationGate activationGate,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeStorageManifestValidator manifestValidator,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics,
            HomeStorageScreenSlotResolver slotResolver,
            HomeStorageTransferExecutor transferExecutor,
            AutoDepositExactOpenContainerBinding binding,
            StoreHomeCandidateRejector candidateRejector,
            StoreHomeOperationTerminator terminator,
            StoreHomeTransferResultHandler transferResultHandler) {
        this.state = Objects.requireNonNull(state, "state");
        this.activationGate = Objects.requireNonNull(activationGate, "activationGate");
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.manifestValidator = Objects.requireNonNull(
                manifestValidator, "manifestValidator"
        );
        this.manifestStaleDiagnostics = Objects.requireNonNull(
                manifestStaleDiagnostics, "manifestStaleDiagnostics"
        );
        this.slotResolver = Objects.requireNonNull(slotResolver, "slotResolver");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
        this.binding = Objects.requireNonNull(binding, "binding");
        this.candidateRejector = Objects.requireNonNull(
                candidateRejector, "candidateRejector"
        );
        this.terminator = Objects.requireNonNull(terminator, "terminator");
        this.transferResultHandler = Objects.requireNonNull(
                transferResultHandler, "transferResultHandler"
        );
    }

    public Task tick(Task diagnosticOwner, AltoClef mod) {
        if (state.lifecycle().phase() != StoreHomePhase.REVALIDATE_AFTER_RESUME) {
            state.lifecycle().transitionTo(StoreHomePhase.VALIDATE_CONTAINER);
        }
        HomeStorageContainerActivation activation = activationGate.evaluate(
                mod,
                state.session().current().candidate(),
                state.context().current(),
                false
        );
        if (!activation.ready()
                || !state.session().current().matchesActivation(activation)) {
            String reason = activation.ready()
                    ? "trusted_gui_handler_rebound"
                    : activation.reason();
            if (state.session().current().pendingTransfer().isPresent()
                    || transferExecutor.hasPending()) {
                terminator.finish(
                        diagnosticOwner,
                        mod,
                        StoreHomeResult.TRANSFER_UNCONFIRMED,
                        "pending_session_binding_lost:" + reason
                );
            } else {
                candidateRejector.reject(
                        diagnosticOwner,
                        mod,
                        reason,
                        StoreHomeCandidateFailureKind.UNAVAILABLE
                );
            }
            return null;
        }

        Optional<HomeStorageInventorySnapshot> current = snapshotReader.capture(mod);
        HomeStorageManifestValidation validation = current
                .map(snapshot -> manifestValidator.validate(
                        state.session().current().baseline(),
                        state.session().current().progress(),
                        snapshot,
                        state.session().current().pendingLogicalSlot()
                ))
                .orElseGet(() -> HomeStorageManifestValidation.unavailable(
                        "player_inventory_snapshot_unavailable"
                ));
        if (!validation.valid()) {
            manifestStaleDiagnostics.recordRootStale(
                    diagnosticOwner,
                    mod,
                    state.lifecycle().phase(),
                    validation,
                    state.session().current().progress(),
                    slotResolver,
                    transferExecutor,
                    state.session().current().candidate(),
                    binding
            );
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.MANIFEST_STALE,
                    validation.reason() + ":slot=" + validation.logicalSlot()
            );
            return null;
        }
        if (state.session().current().progress().complete()
                && state.session().current().pendingTransfer().isEmpty()
                && !transferExecutor.hasPending()) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.COMPLETED,
                    "manifest_complete"
            );
            return null;
        }

        HomeStorageManifestStep step =
                state.session().current().progress().currentStep().orElse(null);
        if (step == null) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.COMPLETED,
                    "manifest_complete"
            );
            return null;
        }
        state.lifecycle().transitionTo(transferExecutor.hasPending()
                ? StoreHomePhase.VERIFY_TRANSFER
                : StoreHomePhase.TRANSFER_EXACT_SLOTS);
        return transferResultHandler.handle(
                diagnosticOwner,
                mod,
                step,
                transferExecutor.tick(
                        mod,
                        state.session().current().candidate().destination(),
                        binding,
                        step,
                        state.session().current().progress().expectedCount(step),
                        state.session().current().progress().manifestStepIndex(step)
                )
        );
    }
}
