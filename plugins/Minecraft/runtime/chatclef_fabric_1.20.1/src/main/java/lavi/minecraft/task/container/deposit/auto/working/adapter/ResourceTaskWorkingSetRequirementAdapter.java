package lavi.minecraft.task.container.deposit.auto.working.adapter;

import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetRequirementAccumulator;

public final class ResourceTaskWorkingSetRequirementAdapter implements TaskWorkingSetRequirementAdapter {
    @Override
    public boolean supports(Task task) {
        return task instanceof ResourceTask;
    }

    @Override
    public boolean collect(Task task, WorkingSetRequirementAccumulator accumulator) {
        ItemTarget[] targets = ((ResourceTask) task).getItemTargets();
        if (targets == null) {
            return false;
        }
        for (ItemTarget target : targets) {
            accumulator.reserveTarget(target);
        }
        return true;
    }
}
