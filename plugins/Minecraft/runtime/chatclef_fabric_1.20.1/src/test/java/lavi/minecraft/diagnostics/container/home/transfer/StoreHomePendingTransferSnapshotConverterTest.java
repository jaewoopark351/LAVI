package lavi.minecraft.diagnostics.container.home.transfer;

import lavi.minecraft.diagnostics.container.home.StoreHomePendingTransferSnapshot;
import lavi.minecraft.task.container.home.execution.transfer.HomeStoragePendingTransferObservation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Verify pending diagnostics conversion without invoking transfer behavior.
class StoreHomePendingTransferSnapshotConverterTest {
    private final StoreHomePendingTransferSnapshotConverter converter =
            new StoreHomePendingTransferSnapshotConverter();

    @Test
    void emptyObservationProducesTheCanonicalNoneSnapshot() {
        StoreHomePendingTransferSnapshot snapshot = converter.convert(
                Optional.empty()
        );

        assertFalse(snapshot.pending());
        assertEquals(-1, snapshot.logicalSlot());
        assertEquals(-1, snapshot.sourceWindowSlot());
        assertEquals(0, snapshot.elapsedTicks());
    }

    @Test
    void presentObservationPreservesEveryPendingField() {
        StoreHomePendingTransferSnapshot snapshot = converter.convert(Optional.of(
                new HomeStoragePendingTransferObservation(
                        "world:dimension:10,64,12",
                        7,
                        34,
                        21,
                        48,
                        9
                )
        ));

        assertTrue(snapshot.pending());
        assertEquals("world:dimension:10,64,12", snapshot.destinationKey());
        assertEquals(7, snapshot.logicalSlot());
        assertEquals(34, snapshot.sourceWindowSlot());
        assertEquals(21, snapshot.sourceCountBefore());
        assertEquals(48, snapshot.destinationCountBefore());
        assertEquals(9, snapshot.elapsedTicks());
    }
}
