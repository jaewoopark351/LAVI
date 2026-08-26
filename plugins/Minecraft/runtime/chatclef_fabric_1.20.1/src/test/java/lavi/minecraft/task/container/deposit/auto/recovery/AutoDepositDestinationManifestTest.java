package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.util.Dimension;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static lavi.minecraft.testsupport.TestItems.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutoDepositDestinationManifestTest {
    @Test
    void recordsOnlyPositiveConfirmedDeltasPerDestination() {
        Item item = item();
        BlockPos position = new BlockPos(4, 70, -3);
        AutoDepositDestinationManifest manifest = new AutoDepositDestinationManifest(
                new Object(), Dimension.OVERWORLD, 19L
        );

        manifest.record(position, item, 5);
        manifest.record(position, item, 3);
        manifest.record(position, item, -9);

        assertEquals(8, manifest.confirmedCount(position, item));
        assertEquals(Map.of(item, 8), manifest.confirmedAt(position));
        assertThrows(UnsupportedOperationException.class,
                () -> manifest.confirmedAt(position).put(item, 1));
    }
}
