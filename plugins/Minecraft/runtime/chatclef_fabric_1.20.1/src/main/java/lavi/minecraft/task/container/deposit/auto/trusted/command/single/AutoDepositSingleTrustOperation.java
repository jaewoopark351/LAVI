package lavi.minecraft.task.container.deposit.auto.trusted.command.single;

import adris.altoclef.AltoClef;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Expose one exact single-destination registration operation to the command facade.
@FunctionalInterface
public interface AutoDepositSingleTrustOperation {
    void register(AltoClef mod);
}
