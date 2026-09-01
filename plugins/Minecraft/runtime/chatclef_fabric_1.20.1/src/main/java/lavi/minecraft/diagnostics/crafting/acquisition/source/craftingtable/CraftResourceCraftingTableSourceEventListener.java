package lavi.minecraft.diagnostics.crafting.acquisition.source.craftingtable;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;

//20260901_kpopmodder: Expose one bounded crafting-table aggregate as a reference only.
public interface CraftResourceCraftingTableSourceEventListener {
    void onRouteAggregate(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String observedEventName,
            String observedReason,
            String observedStateKey,
            String routeKey,
            int observationCount,
            long stableTicks,
            Object[] branchFields
    );
}
