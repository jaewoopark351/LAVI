package adris.altoclef.player2api;

import java.util.ArrayList;
import java.util.List;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementObserver;
import net.minecraft.item.Items;

public class AgentCommandUtils {
    public static ItemTarget[] addPresentItemsToTargets(ItemTarget[] items) {
        List<ItemTarget> resultTargets = new ArrayList<>();
        for (ItemTarget target : items) {
            int requestedCount = target.getTargetCount();
            // append to current count so this is workable with the agent
            int currentItemCount = AltoClef.getInstance().getItemStorage()
                    .getItemCountInventoryOnly(target.getMatches());
            int targetItemCount = requestedCount + currentItemCount;

            //20260901_kpopmodder: Observe the existing quantity decision without changing its result.
            if (ChatClefDiagnostics.isBoundaryEnabled()) {
                CraftResourceRequirementObserver.observeCapturedQuantity(
                        target,
                        () -> target.matches(Items.IRON_PICKAXE)
                                ? "minecraft:iron_pickaxe"
                                : "UNAVAILABLE",
                        requestedCount,
                        currentItemCount,
                        targetItemCount
                );
            }

            resultTargets.add(new ItemTarget(target, targetItemCount));
        }
        return resultTargets.toArray(new ItemTarget[0]);
    }
}
