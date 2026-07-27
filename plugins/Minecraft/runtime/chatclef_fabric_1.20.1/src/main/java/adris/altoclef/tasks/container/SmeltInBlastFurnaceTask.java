package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.multiversion.versionedfields.Items;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.BlastFurnaceSlot;
import net.minecraft.item.Item;
import net.minecraft.screen.BlastFurnaceScreenHandler;

/**
 * Smelt in a blast furnace, placing a blast furnace and collecting fuel as needed.
 */
public class SmeltInBlastFurnaceTask extends AbstractSmeltInContainerTask<SmeltInBlastFurnaceTask.DoSmeltInBlastFurnaceTask> {

    public SmeltInBlastFurnaceTask(SmeltTarget[] targets) {
        super(targets, Blocks.BLAST_FURNACE, new DoSmeltInBlastFurnaceTask(targets[0]), "SmeltInBlastFurnaceTask", "BlastFurnace");
    }

    public SmeltInBlastFurnaceTask(SmeltTarget target) {
        this(new SmeltTarget[]{target});
    }

    static class DoSmeltInBlastFurnaceTask extends AbstractDoSmeltInContainerTask {

        public DoSmeltInBlastFurnaceTask(SmeltTarget target) {
            super(
                    target,
                    Blocks.BLAST_FURNACE,
                    Items.BLAST_FURNACE,
                    BlastFurnaceScreenHandler.class,
                    BlastFurnaceSlot.INPUT_SLOT_MATERIALS,
                    BlastFurnaceSlot.INPUT_SLOT_FUEL,
                    BlastFurnaceSlot.OUTPUT_SLOT,
                    "DoSmeltInBlastFurnaceTask",
                    "blast furnace"
            );
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (hasActiveSmeltingCache()) {
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 11 &&
                    mod.getItemStorage().getItemCount(Items.RAW_IRON) > 5) {
                double cost = 100.0 - 90.0 * (((double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE})
                        / 8.0) + ((double) mod.getItemStorage().getItemCount(Items.RAW_IRON) / 5.0));
                return Math.max(cost, 10.0);
            }
            return StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD) ? 50.0 : 100.0;
        }
    }
}
