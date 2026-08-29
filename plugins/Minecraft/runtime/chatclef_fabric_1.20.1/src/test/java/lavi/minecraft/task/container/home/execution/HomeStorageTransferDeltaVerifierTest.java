package lavi.minecraft.task.container.home.execution;

import lavi.minecraft.task.container.home.execution.transfer.delta.HomeStorageTransferDeltaStatus;
import lavi.minecraft.task.container.home.execution.transfer.delta.HomeStorageTransferDeltaVerification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260827_kpopmodder: Added focused tests for paired trusted transfer evidence.
class HomeStorageTransferDeltaVerifierTest {
    private final HomeStorageTransferDeltaVerifier verifier =
            new HomeStorageTransferDeltaVerifier();

    @Test
    void confirmsOnlyEqualSourceLossAndDestinationGain() {
        HomeStorageTransferDeltaVerification result = verifier.verify(
                64, 20, 10, 54
        );

        assertEquals(HomeStorageTransferDeltaStatus.CONFIRMED, result.status());
        assertEquals(44, result.sourceDelta());
        assertEquals(44, result.destinationDelta());
    }

    @Test
    void keepsOneSidedUpdateWaitingAndRejectsMismatchOrReverse() {
        assertEquals(HomeStorageTransferDeltaStatus.WAITING,
                verifier.verify(64, 20, 10, 10).status());
        assertEquals(HomeStorageTransferDeltaStatus.MISMATCH,
                verifier.verify(64, 20, 10, 40).status());
        assertEquals(HomeStorageTransferDeltaStatus.REVERSED,
                verifier.verify(20, 21, 10, 10).status());
    }
}
