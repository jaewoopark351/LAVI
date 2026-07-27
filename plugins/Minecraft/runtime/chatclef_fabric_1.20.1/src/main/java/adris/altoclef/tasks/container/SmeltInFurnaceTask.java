package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.FurnaceSlot;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;

/**
 * Smelt in a furnace, placing a furnace and collecting fuel as needed.
 */
public class SmeltInFurnaceTask extends AbstractSmeltInContainerTask<SmeltInFurnaceTask.DoSmeltInFurnaceTask> {

    public SmeltInFurnaceTask(SmeltTarget[] targets) {
        super(targets, Blocks.FURNACE, new DoSmeltInFurnaceTask(targets[0]), "SmeltInFurnaceTask", "Furnace");
    }

    public SmeltInFurnaceTask(SmeltTarget target) {
        this(new SmeltTarget[]{target});
    }

    static class DoSmeltInFurnaceTask extends AbstractDoSmeltInContainerTask {

        public DoSmeltInFurnaceTask(SmeltTarget target) {
            super(
                    target,
                    Blocks.FURNACE,
                    Items.FURNACE,
                    FurnaceScreenHandler.class,
                    FurnaceSlot.INPUT_SLOT_MATERIALS,
                    FurnaceSlot.INPUT_SLOT_FUEL,
                    FurnaceSlot.OUTPUT_SLOT,
                    "DoSmeltInFurnaceTask",
                    "furnace"
            );
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (hasActiveSmeltingCache()) {
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 8) {
                double cost = 100.0 - 90.0 * (double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE}) / 8.0;
                return Math.max(cost, 10.0);
            }
            return StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD) ? 50.0 : 100.0;
        }
    }
}
