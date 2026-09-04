package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public enum AutoDepositBulkChestPart {
    NONE,
    SINGLE,
    LEFT,
    RIGHT;

    public boolean doubleHalf() {
        return this == LEFT || this == RIGHT;
    }

    public boolean complements(AutoDepositBulkChestPart other) {
        return (this == LEFT && other == RIGHT)
                || (this == RIGHT && other == LEFT);
    }
}
