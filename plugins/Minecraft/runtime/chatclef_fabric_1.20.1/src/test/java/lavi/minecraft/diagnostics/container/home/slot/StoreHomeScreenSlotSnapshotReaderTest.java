package lavi.minecraft.diagnostics.container.home.slot;

import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.slot.HomeStorageScreenSlotInspection;
import lavi.minecraft.task.container.home.execution.slot.HomeStorageScreenSlotView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Added focused tests for diagnostic slot-match inspection.
class StoreHomeScreenSlotSnapshotReaderTest {
    private final StoreHomeScreenSlotSnapshotReader reader =
            new StoreHomeScreenSlotSnapshotReader(
                    new HomeStorageScreenSlotResolver()
            );

    @Test
    void reportsZeroOneAndMultipleMatchesWithoutChangingResolution() {
        HomeStorageScreenSlotInspection missing =
                reader.inspectUnique(List.of(), 8);
        HomeStorageScreenSlotInspection unique = reader.inspectUnique(
                List.of(new HomeStorageScreenSlotView(62, true, 8)), 8
        );
        HomeStorageScreenSlotInspection duplicate = reader.inspectUnique(
                List.of(
                        new HomeStorageScreenSlotView(62, true, 8),
                        new HomeStorageScreenSlotView(98, true, 8)
                ),
                8
        );

        assertEquals(0, missing.matchCount());
        assertTrue(missing.resolvedWindowSlot().isEmpty());
        assertEquals(1, unique.matchCount());
        assertEquals(62, unique.resolvedWindowSlot().orElseThrow());
        assertEquals(2, duplicate.matchCount());
        assertTrue(duplicate.resolvedWindowSlot().isEmpty());
    }
}
