package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260907_kpopmodder: Prove both before/after observations include inventory and cursor counts once.
class FabricChatClefInventoryAndCursorTargetCounterTest {
    @Test
    void beforeAndAfterEachIncludeEveryInventorySlotAndTheCursor() {
        AtomicInteger inventoryReads = new AtomicInteger();
        AtomicInteger cursorReads = new AtomicInteger();
        int[] beforeInventory = {1, 0, 2};
        int[] afterInventory = {2, 1, 3};

        int before = FabricChatClefInventoryAndCursorTargetCounter.count(
                beforeInventory.length,
                index -> {
                    inventoryReads.incrementAndGet();
                    return beforeInventory[index];
                },
                () -> {
                    cursorReads.incrementAndGet();
                    return 1;
                }
        );
        int after = FabricChatClefInventoryAndCursorTargetCounter.count(
                afterInventory.length,
                index -> {
                    inventoryReads.incrementAndGet();
                    return afterInventory[index];
                },
                () -> {
                    cursorReads.incrementAndGet();
                    return 2;
                }
        );

        assertEquals(4, before);
        assertEquals(8, after);
        assertEquals(4, after - before);
        assertEquals(6, inventoryReads.get());
        assertEquals(2, cursorReads.get());
    }

    @Test
    void aggregateOverflowFailsClosedAtTheReaderBoundary() {
        assertThrows(
                ArithmeticException.class,
                () -> FabricChatClefInventoryAndCursorTargetCounter.count(
                        1,
                        ignored -> Integer.MAX_VALUE,
                        () -> 1
                )
        );
    }
}
