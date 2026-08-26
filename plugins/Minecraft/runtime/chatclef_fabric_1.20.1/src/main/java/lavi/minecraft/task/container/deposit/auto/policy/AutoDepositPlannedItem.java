package lavi.minecraft.task.container.deposit.auto.policy;

import net.minecraft.item.Item;

final class AutoDepositPlannedItem {
    private final Item item;
    private final String itemId;
    private final int count;
    private final int expectedFreedSlots;

    AutoDepositPlannedItem(Item item, String itemId, int count, int expectedFreedSlots) {
        this.item = item;
        this.itemId = itemId;
        this.count = count;
        this.expectedFreedSlots = expectedFreedSlots;
    }

    Item item() {
        return item;
    }

    String itemId() {
        return itemId;
    }

    int count() {
        return count;
    }

    int expectedFreedSlots() {
        return expectedFreedSlots;
    }
}
