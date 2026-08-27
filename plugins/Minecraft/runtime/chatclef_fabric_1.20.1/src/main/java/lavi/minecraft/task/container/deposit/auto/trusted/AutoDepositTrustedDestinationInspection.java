package lavi.minecraft.task.container.deposit.auto.trusted;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Return all eligible trusted candidates instead of one cache-approved position.
public final class AutoDepositTrustedDestinationInspection {
    private final List<AutoDepositTrustedDestinationCandidate> candidates;
    private final long repositoryRevision;
    private final String capacityState;

    AutoDepositTrustedDestinationInspection(
            List<AutoDepositTrustedDestinationCandidate> candidates,
                                            long repositoryRevision,
                                            String capacityState) {
        this.candidates = List.copyOf(candidates);
        this.repositoryRevision = repositoryRevision;
        this.capacityState = Objects.requireNonNull(capacityState, "capacityState");
    }

    public static AutoDepositTrustedDestinationInspection notRequired() {
        return new AutoDepositTrustedDestinationInspection(List.of(), 0L, "not_required");
    }

    public Optional<AutoDepositTrustedDestinationSelection> selection() {
        return candidates.stream().findFirst().map(candidate ->
                new AutoDepositTrustedDestinationSelection(
                        candidate.position(), candidate.cachedEmptySlots()
                ));
    }

    public List<AutoDepositTrustedDestinationCandidate> candidates() {
        return candidates;
    }

    public long repositoryRevision() {
        return repositoryRevision;
    }

    public String capacityState() {
        return capacityState;
    }
}
