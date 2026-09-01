package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.source.furnace.CraftResourceFurnaceSourceEventObserver;
import net.minecraft.item.ItemStack;

//20260814_kpopmodder: Added bounded material-progress diagnostics for cooked beef acquisition stalls.
final class FurnaceMaterialProgressDiagnostics {
    private FurnaceMaterialProgressDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    ItemTarget materialTarget,
                    ItemTarget outputTarget,
                    String currentGate,
                    int inventoryMaterialCount,
                    int inventoryOutputCount,
                    int materialsNeeded,
                    double inventoryFuelCount,
                    double fuelNeeded,
                    boolean materialGateSatisfied,
                    boolean fuelGateSatisfied,
                    ItemStack cachedMaterialSlot,
                    ItemStack cachedFuelSlot,
                    ItemStack cachedOutputSlot,
                    double burningFuelCount,
                    double burnPercentage) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String materialSlotSummary = ChatClefDiagnostics.itemStackSummary(cachedMaterialSlot);
        String fuelSlotSummary = ChatClefDiagnostics.itemStackSummary(cachedFuelSlot);
        String outputSlotSummary = ChatClefDiagnostics.itemStackSummary(cachedOutputSlot);
        String fingerprint = FurnaceDiagnosticEmitter.joinFingerprint(
                "SMELT_MATERIAL_PROGRESS_SNAPSHOT",
                normalize(currentGate),
                Integer.toString(inventoryMaterialCount),
                Integer.toString(inventoryOutputCount),
                Integer.toString(materialsNeeded),
                Double.toString(inventoryFuelCount),
                Double.toString(fuelNeeded),
                Boolean.toString(materialGateSatisfied),
                Boolean.toString(fuelGateSatisfied),
                materialSlotSummary,
                fuelSlotSummary,
                outputSlotSummary,
                Double.toString(burningFuelCount),
                Double.toString(burnPercentage)
        );
        boolean sourceEmissionCompleted = FurnaceDiagnosticEmitter.emit("SMELT_MATERIAL_PROGRESS_SNAPSHOT", "smelt_material_progress_snapshot", task,
                "smelt_material_progress|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "smelt_material_progress_observer",
                        "trigger", "material_or_output_progress_changed_or_summary",
                        "currentGate", normalize(currentGate),
                        "materialTarget", materialTarget,
                        "outputTarget", outputTarget,
                        "inventoryMaterialCount", inventoryMaterialCount,
                        "inventoryOutputCount", inventoryOutputCount,
                        "materialsNeeded", materialsNeeded,
                        "materialDeficit", materialsNeeded - inventoryMaterialCount,
                        "inventoryFuelCount", inventoryFuelCount,
                        "fuelNeeded", fuelNeeded,
                        "fuelDeficit", fuelNeeded - inventoryFuelCount,
                        "materialGateSatisfied", materialGateSatisfied,
                        "fuelGateSatisfied", fuelGateSatisfied,
                        "cachedMaterialSlot", materialSlotSummary,
                        "cachedFuelSlot", fuelSlotSummary,
                        "cachedOutputSlot", outputSlotSummary,
                        "burningFuelCount", burningFuelCount,
                        "burnPercentage", burnPercentage,
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getPathingBehavior().isPathing()),
                        "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getCustomGoalProcess().isActive())
                });
        CraftResourceFurnaceSourceEventObserver.observeMaterialProgress(
                task,
                materialTarget,
                outputTarget,
                currentGate,
                inventoryMaterialCount,
                inventoryOutputCount,
                materialsNeeded,
                inventoryFuelCount,
                fuelNeeded,
                materialGateSatisfied,
                fuelGateSatisfied,
                burningFuelCount,
                burnPercentage,
                sourceEmissionCompleted
        );
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
