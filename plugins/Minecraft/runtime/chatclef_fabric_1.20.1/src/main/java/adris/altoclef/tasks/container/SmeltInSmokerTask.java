package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.SmokerSlot;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.SmokerScreenHandler;

/**
 * Smelt in a smoker, placing a smoker and collecting fuel as needed.
 */
public class SmeltInSmokerTask extends AbstractSmeltInContainerTask<SmeltInSmokerTask.DoSmeltInSmokerTask> {

    public SmeltInSmokerTask(SmeltTarget target) {
        super(new SmeltTarget[]{target}, Blocks.SMOKER, new DoSmeltInSmokerTask(target), "SmeltInSmokerTask", "Smoker");
    }

    @Override
    public SmeltTarget[] getTargets() {
        return new SmeltTarget[]{getTargetArray()[0]};
    }

    static class DoSmeltInSmokerTask extends AbstractDoSmeltInContainerTask {

        public DoSmeltInSmokerTask(SmeltTarget target) {
            super(
                    target,
                    Blocks.SMOKER,
                    Items.SMOKER,
                    SmokerScreenHandler.class,
                    SmokerSlot.INPUT_SLOT_MATERIALS,
                    SmokerSlot.INPUT_SLOT_FUEL,
                    SmokerSlot.OUTPUT_SLOT,
                    "DoSmeltInSmokerTask",
                    "smoker"
            );
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (hasActiveSmeltingCache()) {
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 8 &&
                    mod.getItemStorage().getItemCount(ItemHelper.LOG) > 4) {
                double cost = 100.0 - 90.0 * (((double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE})
                        / 8.0) + ((double) mod.getItemStorage().getItemCount(ItemHelper.LOG) / 4.0));
                return Math.max(cost, 10.0);
            }
            return StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD) ? 50.0 : 100.0;
        }
    }
}
