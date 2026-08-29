package lavi.minecraft.task.container.home.execution.transfer;

import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureObservation;
import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureStage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260829_kpopmodder: Preserve the extracted transfer result contract and status order.
class HomeStorageTransferResultTest {
    @Test
    void statusOrderMatchesTheOriginalExecutorContract() {
        assertArrayEquals(
                new HomeStorageTransferStatus[]{
                        HomeStorageTransferStatus.WAITING,
                        HomeStorageTransferStatus.CLICK_REQUESTED,
                        HomeStorageTransferStatus.TRANSFERRED,
                        HomeStorageTransferStatus.CONTAINER_NOT_OPEN,
                        HomeStorageTransferStatus.NO_CAPACITY,
                        HomeStorageTransferStatus.NO_PROGRESS,
                        HomeStorageTransferStatus.MANIFEST_STALE,
                        HomeStorageTransferStatus.CURSOR_NOT_EMPTY,
                        HomeStorageTransferStatus.TRANSFER_UNCONFIRMED
                },
                HomeStorageTransferStatus.values()
        );
    }

    @Test
    void simpleFactoryPreservesDefaultCountsAndReason() {
        HomeStorageTransferResult result = HomeStorageTransferResult.of(
                HomeStorageTransferStatus.WAITING,
                "awaiting_paired_delta"
        );

        assertEquals(HomeStorageTransferStatus.WAITING, result.status());
        assertEquals(0, result.transferredCount());
        assertEquals(-1, result.sourceCountAfter());
        assertEquals("awaiting_paired_delta", result.reason());
        assertNull(result.failureObservation());
    }

    @Test
    void staleFactoryPreservesManifestStaleDefaults() {
        HomeStorageTransferFailureObservation observation =
                new HomeStorageTransferFailureObservation(
                        HomeStorageTransferFailureStage.SLOT_RESOLUTION,
                        64,
                        3,
                        null,
                        false,
                        "player_main_inventory_after_mapping_failure",
                        Optional.empty()
                );
        HomeStorageTransferResult result = HomeStorageTransferResult.stale(
                "logical_slot_mapping_unavailable",
                observation
        );

        assertEquals(HomeStorageTransferStatus.MANIFEST_STALE, result.status());
        assertEquals(0, result.transferredCount());
        assertEquals(-1, result.sourceCountAfter());
        assertEquals("logical_slot_mapping_unavailable", result.reason());
        assertSame(observation, result.failureObservation());
    }
}
