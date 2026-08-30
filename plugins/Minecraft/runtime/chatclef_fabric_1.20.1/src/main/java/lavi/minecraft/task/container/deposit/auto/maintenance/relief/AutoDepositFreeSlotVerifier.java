package lavi.minecraft.task.container.deposit.auto.maintenance.relief;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;

import java.util.Objects;

//20260831_kpopmodder: Evaluate slot relief through the existing read-only pressure boundary.
/** Evaluates occupied-slot relief from one read-only pressure snapshot. */
public final class AutoDepositFreeSlotVerifier {
    private final AutoDepositInventoryPressureSource pressureSource;

    public AutoDepositFreeSlotVerifier(AutoDepositInventoryPressureSource pressureSource) {
        this.pressureSource = Objects.requireNonNull(pressureSource, "pressureSource");
    }

    public AutoDepositFreeSlotVerdict verify(
            AltoClef mod,
            int startingOccupiedSlots,
            int targetReliefSlots) {
        DepositAllInventoryPressureSnapshot current = pressureSource.read(mod).orElse(null);
        int endingOccupiedSlots = current == null
                ? startingOccupiedSlots
                : current.occupiedSlots();
        int freedSlots = Math.max(0, startingOccupiedSlots - endingOccupiedSlots);
        AutoDepositMaintenanceOutcome outcome;
        if (current != null
                && (current.isAtOrBelowLowWater() || freedSlots >= targetReliefSlots)) {
            outcome = AutoDepositMaintenanceOutcome.FULL_RELIEF;
        } else if (freedSlots > 0) {
            outcome = AutoDepositMaintenanceOutcome.PARTIAL_RELIEF;
        } else {
            outcome = AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF;
        }
        return new AutoDepositFreeSlotVerdict(endingOccupiedSlots, freedSlots, outcome);
    }
}
