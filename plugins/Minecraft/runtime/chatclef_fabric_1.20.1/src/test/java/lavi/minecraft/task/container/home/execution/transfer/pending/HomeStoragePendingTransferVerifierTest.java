package lavi.minecraft.task.container.home.execution.transfer.pending;

import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260829_kpopmodder: Preserve pending context priority and the exact timeout boundary.
class HomeStoragePendingTransferVerifierTest {
    private final HomeStoragePendingTransferVerifier verifier =
            new HomeStoragePendingTransferVerifier();
    private final HomeStorageStackFingerprint fingerprint =
            HomeStorageStackFingerprint.of("minecraft:redstone", 0, null);

    @Test
    void contextChangeDoesNotReadTheDestinationDelta() {
        AtomicInteger destinationReads = new AtomicInteger();
        HomeStoragePendingTransferVerification result = verifier.verify(
                pending(0),
                "different-destination",
                8,
                63,
                null,
                fingerprint,
                destinationReads::incrementAndGet,
                40
        );

        assertEquals(HomeStoragePendingTransferStatus.CONTEXT_CHANGED, result.status());
        assertEquals(0, destinationReads.get());
    }

    @Test
    void waitingDeltaTimesOutWhenTheNextElapsedTickReachesTheLimit() {
        HomeStoragePendingTransferVerification result = verifier.verify(
                pending(39),
                "destination-1",
                8,
                63,
                null,
                fingerprint,
                () -> 12,
                40
        );

        assertEquals(HomeStoragePendingTransferStatus.TIMEOUT, result.status());
        assertEquals("paired_delta_timeout", result.reason());
    }

    private static HomeStoragePendingTransferState pending(int elapsedTicks) {
        return new HomeStoragePendingTransferState(
                "destination-1", 8, 63, 64, 12, elapsedTicks
        );
    }
}
