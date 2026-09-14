package lavi.minecraft.task.container.deposit.auto.admission;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoConflictGuard;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.lifecycle.AutoDepositSafetyAdmission;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.working.ActiveTaskWorkingSetResolver;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetResolution;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

//20260914_kpopmodder: Resolve current reservations and native execution rights before creating a fresh plan.
public final class AutoDepositPlanAdmission {
    private final AltoClef mod;
    private final TaskRunner runner;
    private final DepositAllAutoConflictGuard conflicts;
    private final ActiveTaskWorkingSetResolver workingSets;
    private final AutoDepositPolicyEngine policy;

    public AutoDepositPlanAdmission(AltoClef mod, TaskRunner runner,
                                   DepositAllAutoConflictGuard conflicts,
                                   ActiveTaskWorkingSetResolver workingSets,
                                   AutoDepositPolicyEngine policy) {
        this.mod = mod;
        this.runner = runner;
        this.conflicts = conflicts;
        this.workingSets = workingSets;
        this.policy = policy;
    }

    public String controlDeferral(TaskChain owner) {
        UserTaskChain user = mod.getUserTaskChain();
        Task root = currentRoot();
        if (conflicts.isStoreHomeRoot(root) || conflicts.hasExistingDepositTask(root)) return "manual_storage";
        if (AutoDepositSafetyAdmission.claimed(mod)) return "survival_claim";
        TaskChain selected = runner.getCurrentTaskChain();
        if (selected != null && selected != owner && selected != user) return "native_chain_selected";
        return null;
    }

    public AutoDepositAdmissionResult plan(TaskChain owner, DepositAllInventoryPressureSnapshot pressure) {
        Task root = currentRoot();
        String refusal = controlDeferral(owner);
        if (refusal != null) return new AutoDepositAdmissionResult(refusal, root, null, null);
        WorkingSetSnapshot working = null;
        if (root != null) {
            WorkingSetResolution resolution = workingSets.resolve(mod, owner);
            if (resolution.status() != WorkingSetResolution.Status.SUPPORTED) {
                return new AutoDepositAdmissionResult(resolution.reason(), root, null, null);
            }
            working = resolution.snapshot();
        }
        return new AutoDepositAdmissionResult("evaluated", root, working, policy.plan(mod, pressure, working));
    }

    public Task currentRoot() {
        UserTaskChain user = mod.getUserTaskChain();
        return user != null && user.isActive() && !user.isRunningIdleTask() ? user.getCurrentTask() : null;
    }
}
