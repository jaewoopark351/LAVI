package lavi.minecraft.task.container.home.execution;

//20260827_kpopmodder: Verify paired source loss and trusted-container gain without issuing clicks.
public final class HomeStorageTransferDeltaVerifier {
    public Verification verify(
            int sourceBefore,
            int sourceAfter,
            int destinationBefore,
            int destinationAfter) {
        int sourceDelta = sourceBefore - sourceAfter;
        int destinationDelta = destinationAfter - destinationBefore;
        if (sourceDelta < 0 || destinationDelta < 0) {
            return new Verification(Status.REVERSED, sourceDelta, destinationDelta);
        }
        if (sourceDelta > 0 && destinationDelta > 0) {
            return new Verification(
                    sourceDelta == destinationDelta ? Status.CONFIRMED : Status.MISMATCH,
                    sourceDelta,
                    destinationDelta
            );
        }
        return new Verification(Status.WAITING, sourceDelta, destinationDelta);
    }

    public enum Status {
        WAITING,
        CONFIRMED,
        MISMATCH,
        REVERSED
    }

    public record Verification(Status status, int sourceDelta, int destinationDelta) {
    }
}
