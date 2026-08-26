package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lavi.minecraft.testsupport.TestItems.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutoDepositSurplusTargetSelectorTest {
    @Test
    void selectsOnlyCountsAboveTheImmutableReservation() {
        Item working = item();
        Item junk = item();
        Task root = new TestTask();
        WorkingSetSnapshot snapshot = new WorkingSetSnapshot(
                root,
                List.of(root),
                new Object(),
                Dimension.OVERWORLD,
                7L,
                linkedCounts(working, 25, junk, 10),
                linkedCounts(working, 25, junk, 10),
                Map.of(working, 16)
        );

        ItemTarget[] targets = new AutoDepositSurplusTargetSelector().select(snapshot);

        assertEquals(2, targets.length);
        assertEquals(9, countFor(targets, working));
        assertEquals(10, countFor(targets, junk));
        assertEquals(Map.of(working, 1), snapshot.deficits(Map.of(working, 15, junk, 10)));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.reservedCounts().put(junk, 1));
    }

    private static int countFor(ItemTarget[] targets, Item item) {
        for (ItemTarget target : targets) {
            if (target.matches(item)) {
                return target.getTargetCount();
            }
        }
        return 0;
    }

    private static Map<Item, Integer> linkedCounts(Item first,
                                                   int firstCount,
                                                   Item second,
                                                   int secondCount) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        result.put(first, firstCount);
        result.put(second, secondCount);
        return result;
    }

    private static final class TestTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "working-set snapshot test task";
        }
    }
}
