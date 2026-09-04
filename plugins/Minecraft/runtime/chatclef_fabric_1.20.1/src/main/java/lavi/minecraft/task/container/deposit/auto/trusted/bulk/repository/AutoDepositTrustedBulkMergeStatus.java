package lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public enum AutoDepositTrustedBulkMergeStatus {
    MUTATION_REQUIRED,
    NO_CHANGE,
    INVALID_REQUEST,
    PREEXISTING_DOUBLE_CHEST_DUPLICATE;

    public boolean valid() {
        return this == MUTATION_REQUIRED || this == NO_CHANGE;
    }
}
