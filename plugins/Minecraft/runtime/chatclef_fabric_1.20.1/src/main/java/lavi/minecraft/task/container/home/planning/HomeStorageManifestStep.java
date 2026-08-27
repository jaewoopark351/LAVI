package lavi.minecraft.task.container.home.planning;

import java.util.Objects;

//20260827_kpopmodder: Bind one transfer to an exact player slot and immutable stack identity.
public record HomeStorageManifestStep(
        int logicalPlayerInventorySlot,
        HomeStorageStackFingerprint fingerprint,
        int expectedCount,
        TransferMode transferMode,
        HomeStorageDisposition disposition,
        String dispositionReason,
        long loadoutPlanRevision) {

    public HomeStorageManifestStep {
        if (logicalPlayerInventorySlot < 0 || logicalPlayerInventorySlot >= 36) {
            throw new IllegalArgumentException("logical player slot must be in [0, 35]");
        }
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (expectedCount <= 0) {
            throw new IllegalArgumentException("expectedCount must be positive");
        }
        Objects.requireNonNull(transferMode, "transferMode");
        Objects.requireNonNull(disposition, "disposition");
        Objects.requireNonNull(dispositionReason, "dispositionReason");
        if (disposition != HomeStorageDisposition.STORE_HOME) {
            throw new IllegalArgumentException("manifest steps must be STORE_HOME");
        }
    }

    public enum TransferMode {
        WHOLE_STACK_QUICK_MOVE
    }
}
