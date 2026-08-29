package lavi.minecraft.task.container.home.execution;

import lavi.minecraft.task.container.home.execution.transfer.delta.HomeStorageTransferDeltaStatus;
import lavi.minecraft.task.container.home.execution.transfer.delta.HomeStorageTransferDeltaVerification;

//20260827_kpopmodder: Verify paired source loss and trusted-container gain without issuing clicks.
public final class HomeStorageTransferDeltaVerifier {
    public HomeStorageTransferDeltaVerification verify(
            int sourceBefore,
            int sourceAfter,
            int destinationBefore,
            int destinationAfter) {
        int sourceDelta = sourceBefore - sourceAfter;
        int destinationDelta = destinationAfter - destinationBefore;
        if (sourceDelta < 0 || destinationDelta < 0) {
            return new HomeStorageTransferDeltaVerification(
                    HomeStorageTransferDeltaStatus.REVERSED,
                    sourceDelta,
                    destinationDelta
            );
        }
        if (sourceDelta > 0 && destinationDelta > 0) {
            return new HomeStorageTransferDeltaVerification(
                    sourceDelta == destinationDelta
                            ? HomeStorageTransferDeltaStatus.CONFIRMED
                            : HomeStorageTransferDeltaStatus.MISMATCH,
                    sourceDelta,
                    destinationDelta
            );
        }
        return new HomeStorageTransferDeltaVerification(
                HomeStorageTransferDeltaStatus.WAITING,
                sourceDelta,
                destinationDelta
        );
    }
}
