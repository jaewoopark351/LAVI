package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.multiversion.item.ItemVer;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public final class AutoDepositStackMetadataReader {

    public boolean hasPreservedMetadata(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (ItemVer.hasCustomName(stack) || stack.hasEnchantments()) {
            return true;
        }
        return hasPreservedPayload(stack);
    }

    public String fingerprint(ItemStack stack, boolean preservedMetadata) {
        if (!preservedMetadata) {
            return "plain";
        }
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        return Integer.toHexString(normalized.toString().hashCode());
    }

    private boolean hasPreservedPayload(ItemStack stack) {
        //#if MC >= 12005
        var stackComponents = stack.getComponents();
        var defaultComponents = stack.getItem().getComponents();
        return java.util.stream.Stream.concat(
                        stackComponents.getTypes().stream(),
                        defaultComponents.getTypes().stream()
                )
                .distinct()
                .filter(type -> type != net.minecraft.component.DataComponentTypes.DAMAGE)
                .anyMatch(type -> !Objects.equals(
                        stackComponents.get(type), defaultComponents.get(type)
                ));
        //#else
        //$$ net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        //$$ if (nbt == null) return false;
        //$$ return nbt.getKeys().stream().anyMatch(key -> !"Damage".equals(key));
        //#endif
    }
}
