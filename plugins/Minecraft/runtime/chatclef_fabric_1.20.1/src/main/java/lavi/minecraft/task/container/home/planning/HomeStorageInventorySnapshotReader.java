package lavi.minecraft.task.container.home.planning;

import adris.altoclef.AltoClef;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (mod.getPlayer() == null || mod.getPlayer().getInventory() == null) {
            return List.of();
        }

        List<HomeStorageStackSnapshot> snapshots = new ArrayList<>();
        int selectedSlot = mod.getPlayer().getInventory().selectedSlot;
        for (int slot = 0; slot < mod.getPlayer().getInventory().main.size(); slot++) {
            add(mod, snapshots, slot, HomeStorageStackLocation.MAIN,
                    mod.getPlayer().getInventory().main.get(slot), slot == selectedSlot);
        }
        for (int slot = 0; slot < mod.getPlayer().getInventory().armor.size(); slot++) {
            add(mod, snapshots, slot, HomeStorageStackLocation.ARMOR,
                    mod.getPlayer().getInventory().armor.get(slot), false);
        }
        for (int slot = 0; slot < mod.getPlayer().getInventory().offHand.size(); slot++) {
            add(mod, snapshots, slot, HomeStorageStackLocation.OFFHAND,
                    mod.getPlayer().getInventory().offHand.get(slot), false);
        }
        return List.copyOf(snapshots);
    }

    private void add(
            AltoClef mod,
            List<HomeStorageStackSnapshot> snapshots,
            int logicalSlot,
            HomeStorageStackLocation location,
            ItemStack stack,
            boolean selectedMainHand) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        HomeStorageStackFingerprint fingerprint = HomeStorageStackFingerprint.capture(stack);
        HomeStorageItemFacts facts = factsReader.read(stack, fingerprint.itemId());
        boolean explicitlyProtected = mod.getBehaviour() != null
                && mod.getBehaviour().isProtected(stack.getItem());
        snapshots.add(new HomeStorageStackSnapshot(
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
