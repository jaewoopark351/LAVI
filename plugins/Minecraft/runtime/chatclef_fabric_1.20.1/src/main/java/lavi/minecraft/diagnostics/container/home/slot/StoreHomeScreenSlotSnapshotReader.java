package lavi.minecraft.diagnostics.container.home.slot;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.container.home.StoreHomeScreenSlotSnapshot;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.slot.HomeStorageScreenSlotInspection;
import lavi.minecraft.task.container.home.execution.slot.HomeStorageScreenSlotView;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//20260829_kpopmodder: Added this class to isolate read-only STORE_HOME slot diagnostics.
public final class StoreHomeScreenSlotSnapshotReader {
    private final HomeStorageScreenSlotResolver resolver;

    public StoreHomeScreenSlotSnapshotReader(
            HomeStorageScreenSlotResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public HomeStorageScreenSlotInspection inspectUnique(
            List<HomeStorageScreenSlotView> slots,
            int logicalPlayerSlot) {
        int matchCount = 0;
        for (HomeStorageScreenSlotView slot : slots) {
            if (slot.playerInventory() && slot.logicalSlot() == logicalPlayerSlot) {
                matchCount++;
            }
        }
        return new HomeStorageScreenSlotInspection(
                matchCount,
                resolver.findUnique(slots, logicalPlayerSlot)
        );
    }

    public StoreHomeScreenSlotSnapshot inspect(
            AltoClef mod,
            int logicalPlayerSlot) {
        if (mod == null || mod.getPlayer() == null || logicalPlayerSlot < 0
                || logicalPlayerSlot >= mod.getPlayer().getInventory().main.size()) {
            return StoreHomeScreenSlotSnapshot.notObserved(
                    "logical_slot_unavailable"
            );
        }
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        if (handler == null) {
            return StoreHomeScreenSlotSnapshot.notObserved("handler_unavailable");
        }
        List<HomeStorageScreenSlotView> slots = new ArrayList<>(handler.slots.size());
        for (int windowSlot = 0; windowSlot < handler.slots.size(); windowSlot++) {
            net.minecraft.screen.slot.Slot slot = handler.slots.get(windowSlot);
            slots.add(new HomeStorageScreenSlotView(
                    windowSlot,
                    slot.inventory == mod.getPlayer().getInventory(),
                    slot.getIndex()
            ));
        }
        HomeStorageScreenSlotInspection inspection = inspectUnique(
                slots, logicalPlayerSlot
        );
        if (inspection.resolvedWindowSlot().isEmpty()) {
            return StoreHomeScreenSlotSnapshot.observedWithoutUniqueMapping(
                    inspection.matchCount()
            );
        }
        int resolvedWindowSlot = inspection.resolvedWindowSlot().getAsInt();
        net.minecraft.screen.slot.Slot resolved = handler.slots.get(
                resolvedWindowSlot
        );
        return StoreHomeScreenSlotSnapshot.observed(
                inspection.matchCount(),
                resolvedWindowSlot,
                resolved.inventory == mod.getPlayer().getInventory(),
                resolved.getIndex(),
                mod.getPlayer().getInventory().main.get(logicalPlayerSlot),
                resolved.getStack()
        );
    }
}
