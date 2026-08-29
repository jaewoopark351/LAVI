package lavi.minecraft.task.container.home.execution.transfer.pending;

import lavi.minecraft.task.container.home.execution.HomeStorageTransferDeltaVerifier;
import lavi.minecraft.task.container.home.execution.transfer.delta.HomeStorageTransferDeltaStatus;
import lavi.minecraft.task.container.home.execution.transfer.delta.HomeStorageTransferDeltaVerification;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.function.IntSupplier;

//20260829_kpopmodder: Decide only the current verification state of one pending transfer.
public final class HomeStoragePendingTransferVerifier {
    private final HomeStorageTransferDeltaVerifier deltaVerifier;

    public HomeStoragePendingTransferVerifier() {
        this(new HomeStorageTransferDeltaVerifier());
    }

    HomeStoragePendingTransferVerifier(
            HomeStorageTransferDeltaVerifier deltaVerifier) {
        this.deltaVerifier = Objects.requireNonNull(deltaVerifier, "deltaVerifier");
    }

    public HomeStoragePendingTransferVerification verify(
            HomeStoragePendingTransferState pending,
            String destinationKey,
            int logicalSlot,
            int sourceWindowSlot,
            ItemStack source,
            HomeStorageStackFingerprint fingerprint,
            IntSupplier destinationCountAfter,
            int maxUnconfirmedTicks) {
        Objects.requireNonNull(pending, "pending");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(destinationCountAfter, "destinationCountAfter");
        if (!pending.destinationKey().equals(destinationKey)
                || pending.logicalSlot() != logicalSlot
                || pending.sourceWindowSlot() != sourceWindowSlot) {
            return result(
                    HomeStoragePendingTransferStatus.CONTEXT_CHANGED,
                    0,
                    -1,
                    "pending_transfer_context_changed"
            );
        }
        if (source != null && !source.isEmpty() && !fingerprint.matches(source)) {
            return result(
                    HomeStoragePendingTransferStatus.FINGERPRINT_CHANGED,
                    0,
                    source.getCount(),
                    "source_fingerprint_changed_after_click"
            );
        }

        int sourceAfter = source == null || source.isEmpty() ? 0 : source.getCount();
        HomeStorageTransferDeltaVerification delta = deltaVerifier.verify(
                pending.sourceCountBefore(),
                sourceAfter,
                pending.destinationCountBefore(),
                destinationCountAfter.getAsInt()
        );
        if (delta.status() == HomeStorageTransferDeltaStatus.CONFIRMED) {
            return result(
                    HomeStoragePendingTransferStatus.CONFIRMED,
                    delta.sourceDelta(),
                    sourceAfter,
                    "paired_delta_confirmed"
            );
        }
        if (delta.status() == HomeStorageTransferDeltaStatus.MISMATCH) {
            return result(
                    HomeStoragePendingTransferStatus.DELTA_MISMATCH,
                    0,
                    sourceAfter,
                    "paired_delta_mismatch"
            );
        }
        if (delta.status() == HomeStorageTransferDeltaStatus.REVERSED) {
            return result(
                    HomeStoragePendingTransferStatus.DELTA_REVERSED,
                    0,
                    sourceAfter,
                    "paired_delta_reversed"
            );
        }
        long nextElapsed = (long) pending.elapsedTicks() + 1L;
        if (nextElapsed >= maxUnconfirmedTicks) {
            return result(
                    HomeStoragePendingTransferStatus.TIMEOUT,
                    0,
                    sourceAfter,
                    "paired_delta_timeout"
            );
        }
        return result(
                HomeStoragePendingTransferStatus.WAITING,
                0,
                sourceAfter,
                "awaiting_paired_delta"
        );
    }

    private static HomeStoragePendingTransferVerification result(
            HomeStoragePendingTransferStatus status,
            int transferredCount,
            int sourceCountAfter,
            String reason) {
        return new HomeStoragePendingTransferVerification(
                status,
                transferredCount,
                sourceCountAfter,
                reason
        );
    }
}
