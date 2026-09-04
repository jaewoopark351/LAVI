package lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor;

import adris.altoclef.AltoClef;

//20260904_kpopmodder: Added a narrow read port for operation-local bulk player anchors.
@FunctionalInterface
public interface AutoDepositBulkAnchorReader {
    AutoDepositBulkAnchorReadResult read(AltoClef mod);
}
