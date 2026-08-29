package lavi.minecraft.task.container.home.execution.transfer.container;

import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

//20260829_kpopmodder: Calculate only trusted-container counts and insertion capacity.
public final class HomeStorageContainerInventoryCalculator {
    public int count(
            HomeStorageLiveContainerSnapshot live,
            HomeStorageStackFingerprint fingerprint) {
        int count = 0;
        for (Slot slot : live.handler().slots) {
            if (slot.inventory == live.playerInventory()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty() && fingerprint.matches(stack)) {
                count = saturatingAdd(count, stack.getCount());
            }
        }
        return count;
    }

    public int availableCapacity(
            HomeStorageLiveContainerSnapshot live,
            HomeStorageStackFingerprint fingerprint,
            ItemStack source) {
        int capacity = 0;
        for (Slot slot : live.handler().slots) {
            if (slot.inventory == live.playerInventory() || !slot.canInsert(source)) {
                continue;
            }
            ItemStack current = slot.getStack();
            int maximum = Math.min(source.getMaxCount(), slot.getMaxItemCount(source));
            if (current == null || current.isEmpty()) {
                capacity = saturatingAdd(capacity, maximum);
            } else if (fingerprint.matches(current)) {
                capacity = saturatingAdd(
                        capacity,
                        Math.max(0, maximum - current.getCount())
                );
            }
        }
        return capacity;
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }
}
