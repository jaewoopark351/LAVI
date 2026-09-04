package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public record AutoDepositBulkBuildHeight(int bottomY, int topYExclusive) {
    public AutoDepositBulkBuildHeight {
        if (topYExclusive <= bottomY) {
            throw new IllegalArgumentException("topYExclusive must be greater than bottomY");
        }
    }
}
