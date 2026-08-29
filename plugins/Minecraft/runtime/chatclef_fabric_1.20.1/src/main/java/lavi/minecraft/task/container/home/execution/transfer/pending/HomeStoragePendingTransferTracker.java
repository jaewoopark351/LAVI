package lavi.minecraft.task.container.home.execution.transfer.pending;

import lavi.minecraft.task.container.home.execution.transfer.HomeStoragePendingTransferObservation;

import java.util.Optional;
import java.util.OptionalInt;

//20260829_kpopmodder: Own only the lifecycle of one pending exact transfer reference.
public final class HomeStoragePendingTransferTracker {
    private HomeStoragePendingTransferState current;

    public void begin(
            String destinationKey,
            int logicalSlot,
            int sourceWindowSlot,
            int sourceCountBefore,
            int destinationCountBefore) {
        current = new HomeStoragePendingTransferState(
                destinationKey,
                logicalSlot,
                sourceWindowSlot,
                sourceCountBefore,
                destinationCountBefore,
                0
        );
    }

    public boolean hasPending() {
        return current != null;
    }

    public Optional<HomeStoragePendingTransferState> current() {
        return Optional.ofNullable(current);
    }

    public OptionalInt logicalSlot() {
        return current == null
                ? OptionalInt.empty()
                : OptionalInt.of(current.logicalSlot());
    }

    public Optional<HomeStoragePendingTransferObservation> observation() {
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(new HomeStoragePendingTransferObservation(
                current.destinationKey(),
                current.logicalSlot(),
                current.sourceWindowSlot(),
                current.sourceCountBefore(),
                current.destinationCountBefore(),
                current.elapsedTicks()
        ));
    }

    public void advanceTick() {
        if (current == null) {
            return;
        }
        current = new HomeStoragePendingTransferState(
                current.destinationKey(),
                current.logicalSlot(),
                current.sourceWindowSlot(),
                current.sourceCountBefore(),
                current.destinationCountBefore(),
                current.elapsedTicks() == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : current.elapsedTicks() + 1
        );
    }

    public void clear() {
        current = null;
    }
}
