package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

//20260907_kpopmodder: Sum live inventory slots and the cursor through one overflow-checked boundary.
final class FabricChatClefInventoryAndCursorTargetCounter {
    private FabricChatClefInventoryAndCursorTargetCounter() {
    }

    static int count(
            int inventorySize,
            IntUnaryOperator inventorySlotCounter,
            IntSupplier cursorCounter
    ) {
        if (inventorySize < 0) {
            throw new IllegalArgumentException("inventorySize must not be negative");
        }
        Objects.requireNonNull(inventorySlotCounter, "inventorySlotCounter");
        Objects.requireNonNull(cursorCounter, "cursorCounter");
        int count = 0;
        for (int index = 0; index < inventorySize; index++) {
            count = Math.addExact(count, inventorySlotCounter.applyAsInt(index));
        }
        return Math.addExact(count, cursorCounter.getAsInt());
    }
}
