package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public enum AutoDepositTrustedRegistryReadStatus {
    FILE_MISSING_VALID_EMPTY(true),
    VALID_EMPTY(true),
    VALID_POPULATED(true),
    REGISTRY_READ_FAILED(false);

    private final boolean usable;

    AutoDepositTrustedRegistryReadStatus(boolean usable) {
        this.usable = usable;
    }

    public boolean usable() {
        return usable;
    }
}
