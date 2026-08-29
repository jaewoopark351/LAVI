package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.home.execution.slot.HomeStorageScreenSlotView;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

//20260827_kpopmodder: Resolve a logical player slot from live handler ownership instead of fixed offsets.
public final class HomeStorageScreenSlotResolver {
    public OptionalInt resolve(AltoClef mod, int logicalPlayerSlot) {
        if (mod == null || mod.getPlayer() == null || logicalPlayerSlot < 0
                || logicalPlayerSlot >= mod.getPlayer().getInventory().main.size()) {
            return OptionalInt.empty();
        }
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        if (handler == null) {
            return OptionalInt.empty();
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
        return findUnique(slots, logicalPlayerSlot);
    }

    public OptionalInt findUnique(
            List<HomeStorageScreenSlotView> slots,
            int logicalPlayerSlot) {
        int match = -1;
        for (HomeStorageScreenSlotView slot : slots) {
            if (!slot.playerInventory() || slot.logicalSlot() != logicalPlayerSlot) {
                continue;
            }
            if (match >= 0) {
                return OptionalInt.empty();
            }
            match = slot.windowSlot();
        }
        return match < 0 ? OptionalInt.empty() : OptionalInt.of(match);
    }
}
