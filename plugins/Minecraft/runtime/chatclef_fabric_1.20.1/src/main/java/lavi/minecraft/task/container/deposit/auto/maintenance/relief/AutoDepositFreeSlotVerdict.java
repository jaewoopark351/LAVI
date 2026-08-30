package lavi.minecraft.task.container.deposit.auto.maintenance.relief;

import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;

//20260831_kpopmodder: Represent one immutable automatic-deposit slot-relief observation.
/** Immutable result of the automatic-deposit occupied-slot postcondition. */
public record AutoDepositFreeSlotVerdict(
        int endingOccupiedSlots,
        int freedSlots,
        AutoDepositMaintenanceOutcome outcome) {
}
