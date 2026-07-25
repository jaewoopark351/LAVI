package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this runner to equip inventory items into the active hand slot.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;
import net.minecraft.item.Item;

public class LaviEquipItemRunner {

    private final AltoClef mod;
    private final LaviMinecraftItemResolver itemResolver;

    public LaviEquipItemRunner(AltoClef mod) {
        this(mod, new LaviMinecraftItemResolver());
    }

    public LaviEquipItemRunner(
            AltoClef mod,
            LaviMinecraftItemResolver itemResolver
    ) {
        this.mod = mod;
        this.itemResolver = itemResolver;
    }

    public String equip(LaviCommandSpec commandSpec) {
        String itemName = String.valueOf(commandSpec.getRequest().getOrDefault("item", ""));
        Item item = itemResolver.resolve(itemName);
        if (!mod.getSlotHandler().forceEquipItem(item)) {
            throw new IllegalStateException(
                    "Item is not available in inventory: "
                            + itemName
                            + ". Use get-and-equip "
                            + itemName
                            + " 1 to collect it first."
            );
        }
        return "Equipped " + itemName + ".";
    }
}
