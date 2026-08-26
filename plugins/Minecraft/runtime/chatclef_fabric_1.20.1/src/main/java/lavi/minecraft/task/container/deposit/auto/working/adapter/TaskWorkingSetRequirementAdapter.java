package lavi.minecraft.task.container.deposit.auto.working.adapter;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetRequirementAccumulator;

public interface TaskWorkingSetRequirementAdapter {
    boolean supports(Task task);

    boolean collect(Task task, WorkingSetRequirementAccumulator accumulator);
}
