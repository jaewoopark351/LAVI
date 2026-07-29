package adris.altoclef.tasks.container.smelt;

import adris.altoclef.util.ItemTarget;

//20260729_kpopmodder: Added this planner to isolate smelting material and fuel slot count calculations.
public final class SmeltSlotPlanner {
    public int calculateMaterialsNeeded(
            ItemTarget materialTarget,
            ItemTarget outputTarget,
            int outputInventoryCount,
            SmeltContainerSnapshot snapshot
    ) {
        return materialTarget.getTargetCount()
                - outputInventoryCount
                - (materialTarget.matches(snapshot.getMaterialSlot().getItem()) ? snapshot.getMaterialSlot().getCount() : 0)
                - (outputTarget.matches(snapshot.getOutputSlot().getItem()) ? snapshot.getOutputSlot().getCount() : 0);
    }

    public double calculateFuelNeeded(
            boolean ignoreMaterials,
            ItemTarget materialTarget,
            ItemTarget outputTarget,
            int outputInventoryCount,
            double totalPlannedFuelInContainer,
            SmeltContainerSnapshot snapshot
    ) {
        if (ignoreMaterials) {
            int materialCount = materialTarget.matches(snapshot.getMaterialSlot().getItem()) ? snapshot.getMaterialSlot().getCount() : 0;
            return Math.min(materialCount, materialTarget.getTargetCount());
        }
        return materialTarget.getTargetCount()
                - outputInventoryCount
                - (outputTarget.matches(snapshot.getOutputSlot().getItem()) ? snapshot.getOutputSlot().getCount() : 0)
                - totalPlannedFuelInContainer;
    }

    public int calculateNeededMaterialsInSlot(
            ItemTarget materialTarget,
            ItemTarget outputTarget,
            int outputInventoryCount,
            SmeltContainerSnapshot snapshot
    ) {
        return materialTarget.getTargetCount()
                - outputInventoryCount
                - (outputTarget.matches(snapshot.getOutputSlot().getItem()) ? snapshot.getOutputSlot().getCount() : 0);
    }
}
