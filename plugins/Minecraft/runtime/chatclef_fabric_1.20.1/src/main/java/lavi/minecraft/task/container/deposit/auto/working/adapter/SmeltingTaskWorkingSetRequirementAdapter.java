package lavi.minecraft.task.container.deposit.auto.working.adapter;

import adris.altoclef.tasks.container.SmeltInBlastFurnaceTask;
import adris.altoclef.tasks.container.SmeltInFurnaceTask;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetRequirementAccumulator;
import net.minecraft.item.Item;

import java.util.Map;
import java.util.Arrays;
import java.util.stream.Stream;

public final class SmeltingTaskWorkingSetRequirementAdapter implements TaskWorkingSetRequirementAdapter {
    private final Map<Item, Integer> available;

    public SmeltingTaskWorkingSetRequirementAdapter(Map<Item, Integer> available) {
        this.available = Map.copyOf(available);
    }

    @Override
    public boolean supports(Task task) {
        return task instanceof SmeltInFurnaceTask
                || task instanceof SmeltInSmokerTask
                || task instanceof SmeltInBlastFurnaceTask;
    }

    @Override
    public boolean collect(Task task, WorkingSetRequirementAccumulator accumulator) {
        SmeltTarget[] targets;
        if (task instanceof SmeltInFurnaceTask furnaceTask) {
            targets = furnaceTask.getTargets();
        } else if (task instanceof SmeltInSmokerTask smokerTask) {
            targets = smokerTask.getTargets();
        } else {
            targets = ((SmeltInBlastFurnaceTask) task).getTargets();
        }
        if (targets == null) {
            return false;
        }
        for (SmeltTarget target : targets) {
            if (target == null || target.getMaterial() == null) {
                return false;
            }
            Item[] optionalMaterials = target.getOptionalMaterials() == null
                    ? new Item[0]
                    : target.getOptionalMaterials();
            Item[] allMaterials = Stream.concat(
                    Arrays.stream(target.getMaterial().getMatches()),
                    Arrays.stream(optionalMaterials)
            ).toArray(Item[]::new);
            accumulator.reserveTarget(new ItemTarget(
                    allMaterials,
                    target.getMaterial().getTargetCount()
            ));
        }
        available.keySet().stream().filter(ItemHelper::isFuel).forEach(accumulator::reserveAll);
        return true;
    }
}
