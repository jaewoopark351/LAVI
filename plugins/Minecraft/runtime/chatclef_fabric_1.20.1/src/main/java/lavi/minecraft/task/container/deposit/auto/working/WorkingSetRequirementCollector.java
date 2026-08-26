package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.working.adapter.CraftingTaskWorkingSetRequirementAdapter;
import lavi.minecraft.task.container.deposit.auto.working.adapter.ResourceTaskWorkingSetRequirementAdapter;
import lavi.minecraft.task.container.deposit.auto.working.adapter.SmeltingTaskWorkingSetRequirementAdapter;
import lavi.minecraft.task.container.deposit.auto.working.adapter.TaskWorkingSetRequirementAdapter;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class WorkingSetRequirementCollector {

    public WorkingSetResolution collect(AltoClef mod,
                                        Task root,
                                        List<Task> path,
                                        Object worldIdentity,
                                        adris.altoclef.util.Dimension dimension,
                                        long epoch,
                                        Map<Item, Integer> mainCounts,
                                        Map<Item, Integer> preDepositCounts) {
        Objects.requireNonNull(mod, "mod");
        return collect(
                root,
                path,
                worldIdentity,
                dimension,
                epoch,
                mainCounts,
                preDepositCounts,
                mod.getBehaviour()::isProtected
        );
    }

    public WorkingSetResolution collect(Task root,
                                        List<Task> path,
                                        Object worldIdentity,
                                        adris.altoclef.util.Dimension dimension,
                                        long epoch,
                                        Map<Item, Integer> mainCounts,
                                        Map<Item, Integer> preDepositCounts,
                                        Predicate<Item> isProtected) {
        if (!(root instanceof ResourceTask)) {
            return WorkingSetResolution.unsupported("unsupported_user_task_root");
        }

        WorkingSetRequirementAccumulator accumulator = new WorkingSetRequirementAccumulator(preDepositCounts);
        List<TaskWorkingSetRequirementAdapter> adapters = List.of(
                new ResourceTaskWorkingSetRequirementAdapter(),
                new CraftingTaskWorkingSetRequirementAdapter(),
                new SmeltingTaskWorkingSetRequirementAdapter(preDepositCounts)
        );
        for (Task task : path) {
            for (TaskWorkingSetRequirementAdapter adapter : adapters) {
                if (adapter.supports(task)) {
                    if (!adapter.collect(task, accumulator)) {
                        return WorkingSetResolution.unsupported(
                                "unsupported_task_requirements:" + task.getClass().getName()
                        );
                    }
                }
            }
        }
        preDepositCounts.keySet().stream()
                .filter(isProtected)
                .forEach(accumulator::reserveAll);

        return WorkingSetResolution.supported(new WorkingSetSnapshot(
                root,
                path,
                worldIdentity,
                dimension,
                epoch,
                mainCounts,
                preDepositCounts,
                accumulator.requirements()
        ));
    }
}
