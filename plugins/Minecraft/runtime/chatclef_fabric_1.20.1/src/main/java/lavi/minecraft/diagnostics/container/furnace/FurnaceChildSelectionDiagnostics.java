package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.source.furnace.CraftResourceFurnaceSourceEventObserver;

//20260814_kpopmodder: Added bounded child-selection diagnostics for repeated furnace material delegation.
final class FurnaceChildSelectionDiagnostics {
    private FurnaceChildSelectionDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    Task candidateChild,
                    String currentGate,
                    String candidateChildSemanticKey,
                    int inventoryMaterialCount,
                    int inventoryOutputCount,
                    int materialsNeeded,
                    double inventoryFuelCount,
                    double fuelNeeded,
                    boolean materialGateSatisfied,
                    boolean fuelGateSatisfied) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String normalizedGate = normalize(currentGate);
        String normalizedSemanticKey = normalize(candidateChildSemanticKey);
        String candidateChildClass = FurnaceDiagnosticEmitter.taskClass(candidateChild);
        String fingerprint = FurnaceDiagnosticEmitter.joinFingerprint(
                "SMELT_CHILD_SELECTION_STATE",
                normalizedGate,
                normalizedSemanticKey,
                candidateChildClass,
                Integer.toString(inventoryMaterialCount),
                Integer.toString(inventoryOutputCount),
                Integer.toString(materialsNeeded),
                Double.toString(inventoryFuelCount),
                Double.toString(fuelNeeded),
                Boolean.toString(materialGateSatisfied),
                Boolean.toString(fuelGateSatisfied)
        );
        boolean sourceEmissionCompleted = FurnaceDiagnosticEmitter.emit("SMELT_CHILD_SELECTION_STATE", "smelt_child_selection_state", task,
                "smelt_child_selection|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "smelt_child_selection_observer",
                        "trigger", "candidate_child_selected_or_summary",
                        "currentGate", normalizedGate,
                        "candidateChildClass", candidateChildClass,
                        "candidateChildInstanceId", FurnaceDiagnosticEmitter.instanceId(candidateChild),
                        "candidateChildSemanticKey", normalizedSemanticKey,
                        "candidateChild", ChatClefDiagnostics.taskSummary(candidateChild),
                        "inventoryMaterialCount", inventoryMaterialCount,
                        "inventoryOutputCount", inventoryOutputCount,
                        "materialsNeeded", materialsNeeded,
                        "materialGateSatisfied", materialGateSatisfied,
                        "inventoryFuelCount", inventoryFuelCount,
                        "fuelNeeded", fuelNeeded,
                        "fuelGateSatisfied", fuelGateSatisfied,
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getPathingBehavior().isPathing()),
                        "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getCustomGoalProcess().isActive())
                });
        if (sourceEmissionCompleted) {
            CraftResourceFurnaceSourceEventObserver.observeChildSelection(
                    task,
                    candidateChild,
                    currentGate,
                    candidateChildSemanticKey,
                    inventoryMaterialCount,
                    inventoryOutputCount,
                    materialsNeeded,
                    inventoryFuelCount,
                    fuelNeeded,
                    materialGateSatisfied,
                    fuelGateSatisfied
            );
        }
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }
}
