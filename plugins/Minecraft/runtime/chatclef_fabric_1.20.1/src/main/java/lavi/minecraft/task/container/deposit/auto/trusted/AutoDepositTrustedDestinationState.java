package lavi.minecraft.task.container.deposit.auto.trusted;

import java.util.Objects;

public final class AutoDepositTrustedDestinationState {
    private final long repositoryRevision;
    private final String capacityState;

    AutoDepositTrustedDestinationState(long repositoryRevision, String capacityState) {
        this.repositoryRevision = repositoryRevision;
        this.capacityState = Objects.requireNonNull(capacityState, "capacityState");
    }

    public long repositoryRevision() {
        return repositoryRevision;
    }

    public String capacityState() {
        return capacityState;
    }
}
