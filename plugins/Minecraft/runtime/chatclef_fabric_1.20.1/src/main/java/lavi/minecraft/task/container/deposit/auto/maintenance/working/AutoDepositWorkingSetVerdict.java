package lavi.minecraft.task.container.deposit.auto.maintenance.working;

import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;

//20260914_kpopmodder: Preserve a typed working-set postcondition without assuming missing reads are empty inventory.
public record AutoDepositWorkingSetVerdict(AutoDepositWorkingSetStatus status, int deficitTypes) {
}
