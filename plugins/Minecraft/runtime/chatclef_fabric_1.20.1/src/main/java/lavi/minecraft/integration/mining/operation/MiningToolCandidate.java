package lavi.minecraft.integration.mining.operation;

import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class MiningToolCandidate {
    private final Slot slot;
    private final ItemStack stack;
    private final int remainingDurability;
    private final boolean hotbarVisible;

    public MiningToolCandidate(Slot slot, ItemStack stack, int remainingDurability, boolean hotbarVisible) {
        this.slot = slot;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.remainingDurability = remainingDurability;
        this.hotbarVisible = hotbarVisible;
    }

    public Slot slot() {
        return slot;
    }

    public ItemStack stack() {
        return stack;
    }

    public Item item() {
        return stack.getItem();
    }

    public int remainingDurability() {
        return remainingDurability;
    }

    public boolean hotbarVisible() {
        return hotbarVisible;
    }
}
