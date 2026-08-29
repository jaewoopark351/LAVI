package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.planning.HomeStoragePlanEntry;
import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;

import java.util.Objects;
import java.util.Optional;

//20260828_kpopmodder: Bind one full immutable player-held capture to the plan derived from it.
public final class HomeStorageActivationBaseline {
    private final HomeStorageInventorySnapshot inventory;
    private final HomeStoragePlan plan;

    public HomeStorageActivationBaseline(
            HomeStorageInventorySnapshot inventory,
            HomeStoragePlan plan) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.plan = Objects.requireNonNull(plan, "plan");
        if (plan.entries().size() != inventory.occupiedStacks().size()) {
            throw new IllegalArgumentException("plan must classify every occupied baseline slot");
        }
        for (HomeStoragePlanEntry entry : plan.entries()) {
            HomeStorageInventorySlotSnapshot slot = inventory.slot(
                    entry.snapshot().location(), entry.snapshot().logicalSlot()
            ).orElseThrow(() -> new IllegalArgumentException(
                    "plan entry has no activation slot"
            ));
            if (!slot.occupied()
                    || slot.count() != entry.snapshot().count()
                    || !slot.fingerprint().orElseThrow().equals(
                    entry.snapshot().fingerprint())) {
                throw new IllegalArgumentException(
                        "plan entry differs from the activation capture"
                );
            }
        }
    }

    public HomeStorageInventorySnapshot inventory() {
        return inventory;
    }

    public HomeStoragePlan plan() {
        return plan;
    }

    public Optional<HomeStoragePlanEntry> entry(
            HomeStorageStackLocation location,
            int logicalSlot) {
        return plan.entries().stream()
                .filter(entry -> entry.snapshot().location() == location)
                .filter(entry -> entry.snapshot().logicalSlot() == logicalSlot)
                .findFirst();
    }

    public Optional<HomeStorageManifestStep> manifestStep(int logicalMainSlot) {
        return plan.manifest().steps().stream()
                .filter(step -> step.logicalPlayerInventorySlot() == logicalMainSlot)
                .findFirst();
    }
}
