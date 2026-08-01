package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.tasktrace.VisibleTaskDiagnostics;
import net.minecraft.item.Items;

// TODO: Make this collect more than just coal. It should smartly pick alternative sources if coal is too far away or if we simply cannot get a wooden pick.
public class CollectFuelTask extends Task {

    private final double targetFuel;

    public CollectFuelTask(double targetFuel) {
        this.targetFuel = targetFuel;
    }

    @Override
    protected void onStart() {
        VisibleTaskDiagnostics.logLifecycle(AltoClef.getInstance(), this, "START", "collect_fuel_start",
                "targetFuel", targetFuel);
    }

    @Override
    protected Task onTick() {

        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                // Just collect coal for now.
                setDebugState("Collecting coal.");
                Task coalTask = TaskCatalogue.getItemTask(Items.COAL, (int) Math.ceil(targetFuel / 8));
                VisibleTaskDiagnostics.logReturnTask(AltoClef.getInstance(), this, coalTask, "collect_fuel_return_coal_task",
                        "dimension=OVERWORLD|targetFuel=" + targetFuel,
                        "targetFuel", targetFuel,
                        "targetCoalCount", (int) Math.ceil(targetFuel / 8),
                        "inventoryCoalCount", AltoClef.getInstance().getItemStorage().getItemCountInventoryOnly(Items.COAL));
                return coalTask;
            }
            case END -> {
                setDebugState("Going to overworld, since, well, no more fuel can be found here.");
                Task dimensionTask = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
                VisibleTaskDiagnostics.logReturnTask(AltoClef.getInstance(), this, dimensionTask, "collect_fuel_return_overworld_task",
                        "dimension=END|targetFuel=" + targetFuel,
                        "targetFuel", targetFuel);
                return dimensionTask;
            }
            case NETHER -> {
                setDebugState("Going to overworld, since we COULD use wood but wood confuses the bot. A bug at the moment.");
                Task dimensionTask = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
                VisibleTaskDiagnostics.logReturnTask(AltoClef.getInstance(), this, dimensionTask, "collect_fuel_return_overworld_task",
                        "dimension=NETHER|targetFuel=" + targetFuel,
                        "targetFuel", targetFuel);
                return dimensionTask;
            }
        }
        setDebugState("INVALID DIMENSION: " + WorldHelper.getCurrentDimension());
        VisibleTaskDiagnostics.logDecision(AltoClef.getInstance(), this, "collect_fuel_invalid_dimension",
                "dimension=" + WorldHelper.getCurrentDimension(),
                "targetFuel", targetFuel,
                "dimension", WorldHelper.getCurrentDimension());
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        VisibleTaskDiagnostics.logLifecycle(AltoClef.getInstance(), this, "STOP", "collect_fuel_stop",
                "targetFuel", targetFuel,
                "interruptTask", interruptTask);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectFuelTask task) {
            return Math.abs(task.targetFuel - targetFuel) < 0.01;
        }
        return false;
    }

    @Override
    public boolean isFinished() {
        AltoClef mod = AltoClef.getInstance();
        int coalCount = mod.getItemStorage().getItemCountInventoryOnly(Items.COAL);
        boolean finished = coalCount >= targetFuel;
        VisibleTaskDiagnostics.logFinishedCheck(mod, this, finished, "collect_fuel_is_finished",
                "targetFuel", targetFuel,
                "inventoryCoalCount", coalCount);
        return finished;
    }

    @Override
    protected String toDebugString() {
        return "Collect Fuel: x" + targetFuel;
    }
}
