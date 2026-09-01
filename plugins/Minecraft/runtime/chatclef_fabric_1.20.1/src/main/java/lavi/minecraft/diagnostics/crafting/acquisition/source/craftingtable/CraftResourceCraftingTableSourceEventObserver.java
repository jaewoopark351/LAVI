package lavi.minecraft.diagnostics.crafting.acquisition.source.craftingtable;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;

//20260901_kpopmodder: Isolate crafting-table aggregate projections from source logging.
public final class CraftResourceCraftingTableSourceEventObserver {
    private static final CraftResourceCraftingTableSourceEventListener NO_OP =
            (task, target, blocks, event, reason, state, key, count, ticks, fields) -> {
            };
    private static volatile CraftResourceCraftingTableSourceEventListener listener = NO_OP;

    private CraftResourceCraftingTableSourceEventObserver() {
    }

    public static void install(CraftResourceCraftingTableSourceEventListener installed) {
        listener = installed == null ? NO_OP : installed;
    }

    public static void observeRouteAggregate(
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            String observedEventName,
            String observedReason,
            String observedStateKey,
            String routeKey,
            int observationCount,
            long stableTicks,
            Object[] branchFields) {
        try {
            listener.onRouteAggregate(
                    task,
                    containerTarget,
                    containerBlocks,
                    observedEventName,
                    observedReason,
                    observedStateKey,
                    routeKey,
                    observationCount,
                    stableTicks,
                    branchFields
            );
        } catch (RuntimeException | LinkageError ignoredDiagnosticFailure) {
            // The optional projection cannot change the authoritative log path.
        }
    }
}
