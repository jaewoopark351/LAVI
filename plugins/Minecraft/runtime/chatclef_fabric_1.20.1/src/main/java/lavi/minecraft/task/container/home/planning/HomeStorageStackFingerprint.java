package lavi.minecraft.task.container.home.planning;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;

import java.util.Objects;

//20260827_kpopmodder: Preserve exact 1.20.1 item identity while keeping stack count separate.
public final class HomeStorageStackFingerprint {
    private final String itemId;
    private final int damage;
    private final NbtCompound nbt;
    private final ItemStack exactStack;

    private HomeStorageStackFingerprint(
            String itemId,
            int damage,
            NbtCompound nbt,
            ItemStack exactStack) {
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.damage = Math.max(0, damage);
        this.nbt = copy(nbt);
        this.exactStack = exactStack == null ? null : normalize(exactStack);
    }

    public static HomeStorageStackFingerprint capture(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot fingerprint an empty stack");
        }
        return new HomeStorageStackFingerprint(
                String.valueOf(Registries.ITEM.getId(stack.getItem())),
                stack.getDamage(),
                null,
                stack
        );
    }

    public static HomeStorageStackFingerprint of(String itemId, int damage, NbtCompound nbt) {
        return new HomeStorageStackFingerprint(itemId, damage, nbt, null);
    }

    public boolean matches(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && exactStack != null
                && itemId.equals(String.valueOf(Registries.ITEM.getId(stack.getItem())))
                && damage == stack.getDamage()
                && ItemStack.areEqual(exactStack, normalize(stack));
    }

    public String itemId() {
        return itemId;
    }

    public int damage() {
        return damage;
    }

    public ItemStack exactStackCopy() {
        return exactStack == null ? null : exactStack.copy();
    }

    public String summary() {
        return itemId + ":" + damage + ":" + Integer.toHexString(hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HomeStorageStackFingerprint fingerprint)) {
            return false;
        }
        if (damage != fingerprint.damage || !itemId.equals(fingerprint.itemId)) {
            return false;
        }
        if (exactStack != null || fingerprint.exactStack != null) {
            return exactStack != null
                    && fingerprint.exactStack != null
                    && ItemStack.areEqual(exactStack, fingerprint.exactStack);
        }
        return Objects.equals(nbt, fingerprint.nbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, damage);
    }

    private static NbtCompound copy(NbtCompound value) {
        return value == null ? null : value.copy();
    }

    private static ItemStack normalize(ItemStack value) {
        ItemStack normalized = value.copy();
        normalized.setCount(1);
        return normalized;
    }
}
