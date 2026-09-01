package lavi.minecraft.diagnostics.crafting.acquisition.requirement;

/**
 * Receives values already captured by the authoritative quantity decision.
 */
@FunctionalInterface
public interface CraftResourceRequirementListener {
    void onCapturedQuantity(
            String requestedItem,
            int requestedCount,
            int currentItemCount,
            int targetItemCount);
}
