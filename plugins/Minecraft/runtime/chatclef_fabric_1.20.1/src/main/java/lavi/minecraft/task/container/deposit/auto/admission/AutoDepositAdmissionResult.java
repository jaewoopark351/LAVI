package lavi.minecraft.task.container.deposit.auto.admission;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlanningResult;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

//20260914_kpopmodder: Preserve current control deferral separately from an evaluated storage plan.
public record AutoDepositAdmissionResult(String reason, Task root,
                                         WorkingSetSnapshot workingSet,
                                         AutoDepositPlanningResult planning) {
    public boolean deferred() { return planning == null; }
}
