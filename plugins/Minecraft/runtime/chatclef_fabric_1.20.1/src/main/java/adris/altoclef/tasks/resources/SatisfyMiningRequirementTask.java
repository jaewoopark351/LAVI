package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import lavi.minecraft.integration.mining.MiningToolReadiness;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;

import java.util.Objects;

/**
 * Make sure we have a tool at or above a mining level.
 */
public class SatisfyMiningRequirementTask extends Task {

    private final MiningRequirement requirement;
    private final BlockState targetState;

    public SatisfyMiningRequirementTask(MiningRequirement requirement) {
        this(requirement, null);
    }

    public SatisfyMiningRequirementTask(MiningRequirement requirement, BlockState targetState) {
        this.requirement = requirement;
        this.targetState = targetState;
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected Task onTick() {
        switch (requirement) {
            case HAND:
                // Will never happen if you program this right
                break;
            case WOOD:
                return TaskCatalogue.getItemTask(Items.WOODEN_PICKAXE, 1);
            case STONE:
                return TaskCatalogue.getItemTask(Items.STONE_PICKAXE, 1);
            case IRON:
                return TaskCatalogue.getItemTask(Items.IRON_PICKAXE, 1);
            case DIAMOND:
                return TaskCatalogue.getItemTask(Items.DIAMOND_PICKAXE, 1);
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof SatisfyMiningRequirementTask task) {
            return task.requirement == requirement && Objects.equals(task.targetState, targetState);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (targetState == null) {
            return "Satisfy Mining Req: " + requirement;
        }
        return "Satisfy Mining Req: " + requirement + " for " + targetState;
    }

    @Override
    public boolean isFinished() {
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        // Target-aware mode finishes only when the actual selector can use a tool for this block.
        if (targetState != null) {
            return MiningToolReadiness.hasSelectableMiningTool(AltoClef.getInstance(), targetState);
        }
        return StorageHelper.miningRequirementMetInventory(requirement);
    }
}
