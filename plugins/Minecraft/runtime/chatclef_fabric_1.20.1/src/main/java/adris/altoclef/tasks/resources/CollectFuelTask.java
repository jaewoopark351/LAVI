package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.item.Items;

// TODO: Make this collect more than just coal. It should smartly pick alternative sources if coal is too far away or if we simply cannot get a wooden pick.
public class CollectFuelTask extends Task {

    private final double targetFuel;
    private static final double COAL_FUEL_AMOUNT = ItemHelper.getFuelAmount(Items.COAL);

    public CollectFuelTask(double targetFuel) {
        this.targetFuel = targetFuel;
    }

    @Override
    protected void onStart() {
        // Nothing
    }

    @Override
    protected Task onTick() {

        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                // Just collect coal for now.
                AltoClef mod = AltoClef.getInstance();
                double fuelRemaining = Math.max(0, targetFuel - StorageHelper.calculateInventoryFuelCount(mod));
                int coalToCollect = COAL_FUEL_AMOUNT <= 0 ? 0 : (int) Math.ceil(fuelRemaining / COAL_FUEL_AMOUNT);
                int coalTarget = mod.getItemStorage().getItemCountInventoryOnly(Items.COAL) + coalToCollect;
                setDebugState("Collecting coal.");
                return TaskCatalogue.getItemTask(Items.COAL, coalTarget);
            }
            case END -> {
                setDebugState("Going to overworld, since, well, no more fuel can be found here.");
                return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
            }
            case NETHER -> {
                setDebugState("Going to overworld, since we COULD use wood but wood confuses the bot. A bug at the moment.");
                return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
            }
        }
        setDebugState("INVALID DIMENSION: " + WorldHelper.getCurrentDimension());
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        // Nothing
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
        return StorageHelper.calculateInventoryFuelCount(AltoClef.getInstance()) >= targetFuel;
    }

    @Override
    protected String toDebugString() {
        return "Collect Fuel: x" + targetFuel;
    }
}
