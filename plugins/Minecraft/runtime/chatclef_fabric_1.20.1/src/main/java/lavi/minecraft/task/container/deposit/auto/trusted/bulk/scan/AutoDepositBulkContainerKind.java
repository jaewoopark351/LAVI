package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public enum AutoDepositBulkContainerKind {
    CHEST,
    TRAPPED_CHEST,
    BARREL,
    OTHER;

    public boolean supported() {
        return this == CHEST || this == TRAPPED_CHEST || this == BARREL;
    }

    public boolean chest() {
        return this == CHEST || this == TRAPPED_CHEST;
    }
}
