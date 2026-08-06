package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260807_kpopmodder: Observe high-level furnace operation gate changes only.
final class FurnaceOperationGateDiagnostics {
    private FurnaceOperationGateDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    String currentGate,
                    int inventoryMaterialCount,
                    int materialsNeeded,
                    boolean materialGateSatisfied,
                    double inventoryFuelCount,
                    double fuelNeeded,
                    boolean fuelGateSatisfied,
                    Object materialsAccessible,
                    boolean containerFlowEligible,
                    int inventoryOutputCount) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        FurnaceTaskDiagnosticState.GateObservation gate = FurnaceTaskDiagnosticState.observeGate(task, currentGate);
        if (!gate.gateChanged) {
            return;
        }
        String fingerprint = FurnaceDiagnosticEmitter.joinFingerprint(
                "FURNACE_OPERATION_GATE_TRANSITION",
                gate.previousGate,
                gate.currentGate,
                Boolean.toString(materialGateSatisfied),
                Boolean.toString(fuelGateSatisfied),
                String.valueOf(materialsAccessible),
                Boolean.toString(containerFlowEligible)
        );
        FurnaceDiagnosticEmitter.emit("FURNACE_OPERATION_GATE_TRANSITION", "furnace_operation_gate_transition", task,
                "furnace_operation_gate|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "furnace_operation_gate_observer",
                        "trigger", "operation_gate_changed",
                        "previousGate", gate.previousGate,
                        "currentGate", gate.currentGate,
                        "gateChanged", gate.gateChanged,
                        "gateTransitionCount", gate.gateTransitionCount,
                        "inventoryMaterialCount", inventoryMaterialCount,
                        "materialsNeeded", materialsNeeded,
                        "materialGateSatisfied", materialGateSatisfied,
                        "inventoryFuelCount", inventoryFuelCount,
                        "fuelNeeded", fuelNeeded,
                        "fuelGateSatisfied", fuelGateSatisfied,
                        "materialsAccessible", materialsAccessible,
                        "containerFlowEligible", containerFlowEligible,
                        "inventoryOutputCount", inventoryOutputCount,
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod)
                });
    }
}
