package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.HomeStorageManifestProgress;
import lavi.minecraft.task.container.home.execution.operation.HomeStorageTransferIdentity;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//20260828_kpopmodder: Own one exact trusted-GUI activation and its local transfer state.
public final class HomeStorageContainerSession {
    private final AutoDepositTrustedDestinationCandidate candidate;
    private final int ordinal;
    private final HomeStorageActivationBaseline baseline;
    private final HomeStorageManifestProgress progress;
    private final Object handlerIdentity;
    private final int syncId;
    private final Optional<HomeStorageTransferIdentity> pendingTransfer;
    private final int nextAttemptOrdinal;

    private HomeStorageContainerSession(
            AutoDepositTrustedDestinationCandidate candidate,
            int ordinal,
            HomeStorageActivationBaseline baseline,
            HomeStorageManifestProgress progress,
            Object handlerIdentity,
            int syncId,
            Optional<HomeStorageTransferIdentity> pendingTransfer,
            int nextAttemptOrdinal) {
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        if (ordinal <= 0) {
            throw new IllegalArgumentException("ordinal must be positive");
        }
        this.ordinal = ordinal;
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.handlerIdentity = Objects.requireNonNull(handlerIdentity, "handlerIdentity");
        if (syncId < 0) {
            throw new IllegalArgumentException("syncId must be non-negative");
        }
        this.syncId = syncId;
        this.pendingTransfer = Objects.requireNonNull(pendingTransfer, "pendingTransfer");
        if (nextAttemptOrdinal <= 0) {
            throw new IllegalArgumentException("nextAttemptOrdinal must be positive");
        }
        this.nextAttemptOrdinal = nextAttemptOrdinal;
    }

    public static HomeStorageContainerSession activate(
            AutoDepositTrustedDestinationCandidate candidate,
            int ordinal,
            HomeStorageActivationBaseline baseline,
            HomeStorageContainerActivation activation) {
        if (!activation.ready()) {
            throw new IllegalArgumentException("session requires a ready activation");
        }
        return new HomeStorageContainerSession(
                candidate,
                ordinal,
                baseline,
                new HomeStorageManifestProgress(baseline.plan().manifest()),
                activation.handlerIdentity().orElseThrow(),
                activation.syncId(),
                Optional.empty(),
                1
        );
    }

    public HomeStorageContainerSession beginTransfer(
            long operationId,
            HomeStorageManifestStep step) {
        if (pendingTransfer.isPresent()) {
            throw new IllegalStateException("session already owns a pending transfer");
        }
        if (progress.currentStep().filter(step::equals).isEmpty()) {
            throw new IllegalArgumentException("only the current manifest step may be requested");
        }
        HomeStorageTransferIdentity identity = new HomeStorageTransferIdentity(
                operationId,
                ordinal,
                plan().revision(),
                nextAttemptOrdinal,
                step.logicalPlayerInventorySlot(),
                candidate.destination().key(),
                handlerIdentity,
                syncId
        );
        return copy(progress, Optional.of(identity), nextAttemptOrdinal + 1);
    }

    public HomeStorageContainerSession committed(
            HomeStorageManifestProgress confirmedProgress) {
        if (pendingTransfer.isEmpty()) {
            throw new IllegalStateException("session has no transfer to commit");
        }
        return copy(confirmedProgress, Optional.empty(), nextAttemptOrdinal);
    }

    public boolean matchesActivation(HomeStorageContainerActivation activation) {
        return activation.ready()
                && activation.handlerIdentity().filter(identity -> identity == handlerIdentity)
                .isPresent()
                && activation.syncId() == syncId;
    }

    public AutoDepositTrustedDestinationCandidate candidate() {
        return candidate;
    }

    public int ordinal() {
        return ordinal;
    }

    public HomeStorageActivationBaseline baseline() {
        return baseline;
    }

    public HomeStoragePlan plan() {
        return baseline.plan();
    }

    public HomeStorageManifestProgress progress() {
        return progress;
    }

    public Optional<HomeStorageTransferIdentity> pendingTransfer() {
        return pendingTransfer;
    }

    public OptionalInt pendingLogicalSlot() {
        return pendingTransfer.isPresent()
                ? OptionalInt.of(pendingTransfer.orElseThrow().logicalPlayerSlot())
                : OptionalInt.empty();
    }

    public boolean ownsPendingTransfer(
            HomeStorageTransferIdentity identity,
            long operationId) {
        return identity != null
                && pendingTransfer.filter(identity::equals).isPresent()
                && identity.operationId() == operationId
                && identity.containerSessionOrdinal() == ordinal
                && identity.planRevision() == plan().revision()
                && identity.destinationKey().equals(candidate.destination().key())
                && identity.handlerIdentity() == handlerIdentity
                && identity.handlerSyncId() == syncId;
    }

    private HomeStorageContainerSession copy(
            HomeStorageManifestProgress newProgress,
            Optional<HomeStorageTransferIdentity> newPendingTransfer,
            int newNextAttemptOrdinal) {
        return new HomeStorageContainerSession(
                candidate,
                ordinal,
                baseline,
                newProgress,
                handlerIdentity,
                syncId,
                newPendingTransfer,
                newNextAttemptOrdinal
        );
    }
}
