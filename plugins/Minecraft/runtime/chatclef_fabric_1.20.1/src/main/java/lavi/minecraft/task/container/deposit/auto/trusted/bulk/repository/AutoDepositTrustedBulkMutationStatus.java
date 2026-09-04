package lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public enum AutoDepositTrustedBulkMutationStatus {
    UPDATED(true),
    NO_CHANGE(true),
    REGISTRY_READ_FAILED(false),
    INVALID_REQUEST(false),
    PREEXISTING_DOUBLE_CHEST_DUPLICATE(false),
    EXTERNAL_MODIFICATION_CONFLICT(false),
    PERSISTENCE_FAILED(false);

    private final boolean success;

    AutoDepositTrustedBulkMutationStatus(boolean success) {
        this.success = success;
    }

    public boolean success() {
        return success;
    }
}
