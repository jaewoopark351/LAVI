package lavi.minecraft.task.container.deposit.auto.trusted;

import java.util.Objects;
import java.util.Optional;

public final class AutoDepositTrustedDestinationInspection {
    private final AutoDepositTrustedDestinationSelection selection;
    private final long repositoryRevision;
    private final String capacityState;

    AutoDepositTrustedDestinationInspection(AutoDepositTrustedDestinationSelection selection,
                                            long repositoryRevision,
                                            String capacityState) {
        this.selection = selection;
        this.repositoryRevision = repositoryRevision;
        this.capacityState = Objects.requireNonNull(capacityState, "capacityState");
    }

    public static AutoDepositTrustedDestinationInspection notRequired() {
        return new AutoDepositTrustedDestinationInspection(null, 0L, "not_required");
    }

    public Optional<AutoDepositTrustedDestinationSelection> selection() {
        return Optional.ofNullable(selection);
    }

    public long repositoryRevision() {
        return repositoryRevision;
    }

    public String capacityState() {
        return capacityState;
    }
}
