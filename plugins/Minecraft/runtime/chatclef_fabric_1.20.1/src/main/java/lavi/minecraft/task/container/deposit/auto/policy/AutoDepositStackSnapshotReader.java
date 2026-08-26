package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.item.ItemVer;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC >= 11903
import net.minecraft.registry.Registries;
//#else
//$$ import net.minecraft.util.registry.Registry;
//#endif

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AutoDepositStackSnapshotReader {
    private final AutoDepositStackMetadataReader metadataReader;
    private final AutoDepositItemRoleClassifier roleClassifier;

    public AutoDepositStackSnapshotReader() {
        this(new AutoDepositStackMetadataReader(), new AutoDepositItemRoleClassifier());
    }

    AutoDepositStackSnapshotReader(AutoDepositStackMetadataReader metadataReader,
                                   AutoDepositItemRoleClassifier roleClassifier) {
        this.metadataReader = Objects.requireNonNull(metadataReader, "metadataReader");
        this.roleClassifier = Objects.requireNonNull(roleClassifier, "roleClassifier");
    }

    public List<AutoDepositStackSnapshot> read(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        if (mod.getPlayer() == null || mod.getPlayer().getInventory() == null) {
            return List.of();
        }

        List<AutoDepositStackSnapshot> result = new ArrayList<>();
        int selectedSlot = mod.getPlayer().getInventory().selectedSlot;
        for (int index = 0; index < mod.getPlayer().getInventory().main.size(); index++) {
            add(mod, result, index, AutoDepositStackLocation.MAIN,
                    mod.getPlayer().getInventory().main.get(index), index == selectedSlot);
        }
        for (int index = 0; index < mod.getPlayer().getInventory().armor.size(); index++) {
            add(mod, result, index, AutoDepositStackLocation.ARMOR,
                    mod.getPlayer().getInventory().armor.get(index), false);
        }
        for (int index = 0; index < mod.getPlayer().getInventory().offHand.size(); index++) {
            add(mod, result, index, AutoDepositStackLocation.OFFHAND,
                    mod.getPlayer().getInventory().offHand.get(index), false);
        }
        return List.copyOf(result);
    }

    private void add(AltoClef mod,
                     List<AutoDepositStackSnapshot> result,
                     int slotIndex,
                     AutoDepositStackLocation location,
                     ItemStack stack,
                     boolean selectedMainHand) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        String itemId = itemId(item);
        AutoDepositItemRole role = roleClassifier.classify(itemId);
        boolean specialMetadata = metadataReader.hasPreservedMetadata(stack);
        boolean blockItem = item instanceof BlockItem;
        boolean fallingBlock = blockItem && ((BlockItem) item).getBlock() instanceof FallingBlock;
        result.add(new AutoDepositStackSnapshot(
                slotIndex,
                location,
                item,
                itemId,
                stack.getCount(),
                stack.getMaxCount(),
                selectedMainHand,
                mod.getBehaviour() != null && mod.getBehaviour().isProtected(item),
                specialMetadata,
                metadataReader.fingerprint(stack, specialMetadata),
                role,
                roleClassifier.equipmentScore(itemId, stack, role),
                ItemVer.isFood(stack),
                blockItem,
                fallingBlock
        ));
    }

    private static String itemId(Item item) {
        //#if MC >= 11903
        return String.valueOf(Registries.ITEM.getId(item));
        //#else
        //$$ return String.valueOf(Registry.ITEM.getId(item));
        //#endif
    }
}
