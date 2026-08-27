package lavi.minecraft.task.container.deposit.auto.trusted;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260827_kpopmodder: Carry an immutable trusted candidate hint into one automatic-deposit operation.
public final class AutoDepositTrustedDestinationCandidate {
    private final AutoDepositTrustedDestination destination;
    private final int cachedEmptySlots;
    private final double distanceSquared;
    private final String observedState;

    public AutoDepositTrustedDestinationCandidate(
            AutoDepositTrustedDestination destination,
            int cachedEmptySlots,
            double distanceSquared,
            String observedState) {
        this.destination = Objects.requireNonNull(destination, "destination");
        this.cachedEmptySlots = Math.max(0, cachedEmptySlots);
        this.distanceSquared = Math.max(0.0, distanceSquared);
        this.observedState = Objects.requireNonNull(observedState, "observedState");
    }

    public AutoDepositTrustedDestination destination() {
        return destination;
    }

    public String destinationId() {
        return destination.destinationId();
    }

    public BlockPos position() {
        return destination.position();
    }

    public int cachedEmptySlots() {
        return cachedEmptySlots;
    }

    public double distanceSquared() {
        return distanceSquared;
    }

    public String observedState() {
        return observedState;
    }
}
