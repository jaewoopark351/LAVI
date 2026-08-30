package lavi.minecraft.task.container.deposit.auto.pressure;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;

import java.util.Optional;

//20260829_kpopmodder: Isolate read-only inventory-pressure observation from automatic lifecycle ownership.
@FunctionalInterface
public interface AutoDepositInventoryPressureSource {
    Optional<DepositAllInventoryPressureSnapshot> read(AltoClef mod);
}
