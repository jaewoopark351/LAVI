package lavi.minecraft.task.container.home.planning;

import adris.altoclef.AltoClef;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Read player inventory state only after an explicit store-home request.
public final class HomeStorageInventorySnapshotReader {
    private final HomeStorageItemFactsReader factsReader;

    public HomeStorageInventorySnapshotReader() {
        this(new HomeStorageItemFactsReader());
    }

    HomeStorageInventorySnapshotReader(HomeStorageItemFactsReader factsReader) {
        this.factsReader = Objects.requireNonNull(factsReader, "factsReader");
    }

    public List<HomeStorageStackSnapshot> read(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        return capture(mod)
                .map(HomeStorageInventorySnapshot::occupiedStacks)
                .orElseGet(List::of);
    }

    //20260828_kpopmodder: Capture every player-held slot and planner fact from one live read.
    public Optional<HomeStorageInventorySnapshot> capture(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null
                || mod.getPlayer().getInventory() == null) {
            return Optional.empty();
        }

        PlayerInventory inventory = mod.getPlayer().getInventory();
        List<HomeStorageInventorySlotSnapshot> slots = new ArrayList<>();
        List<HomeStorageStackSnapshot> occupied = new ArrayList<>();
        int selectedSlot = inventory.selectedSlot;
        if (selectedSlot < 0 || selectedSlot >= inventory.main.size()) {
            return Optional.empty();
        }
        for (int slot = 0; slot < inventory.main.size(); slot++) {
            add(mod, slots, occupied, slot, HomeStorageStackLocation.MAIN,
                    inventory.main.get(slot), slot == selectedSlot);
        }
        for (int slot = 0; slot < inventory.armor.size(); slot++) {
            add(mod, slots, occupied, slot, HomeStorageStackLocation.ARMOR,
                    inventory.armor.get(slot), false);
        }
        for (int slot = 0; slot < inventory.offHand.size(); slot++) {
            add(mod, slots, occupied, slot, HomeStorageStackLocation.OFFHAND,
                    inventory.offHand.get(slot), false);
        }
        return Optional.of(new HomeStorageInventorySnapshot(
                slots,
                occupied,
                selectedSlot
        ));
    }

    private void add(
            AltoClef mod,
            List<HomeStorageInventorySlotSnapshot> slots,
            List<HomeStorageStackSnapshot> occupied,
            int logicalSlot,
            HomeStorageStackLocation location,
            ItemStack stack,
            boolean selectedMainHand) {
        if (stack == null || stack.isEmpty()) {
            slots.add(HomeStorageInventorySlotSnapshot.empty(location, logicalSlot));
            return;
        }
        HomeStorageStackFingerprint fingerprint = HomeStorageStackFingerprint.capture(stack);
        slots.add(HomeStorageInventorySlotSnapshot.occupied(
                location,
                logicalSlot,
                fingerprint,
                stack.getCount()
        ));
        HomeStorageItemFacts facts = factsReader.read(stack, fingerprint.itemId());
        boolean explicitlyProtected = mod.getBehaviour() != null
                && mod.getBehaviour().isProtected(stack.getItem());
        occupied.add(new HomeStorageStackSnapshot(
                logicalSlot,
                location,
                fingerprint,
                stack.getCount(),
                facts.role(),
                facts.capabilityScore(),
                facts.enchantmentScore(),
                facts.remainingDurability(),
                facts.maximumDurability(),
                selectedMainHand,
                explicitlyProtected,
                facts.safeGeneralFood(),
                facts.foodScore()
        ));
    }
}
