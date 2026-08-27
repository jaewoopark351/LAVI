package lavi.minecraft.task.container.deposit.auto.trusted.execution;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//20260827_kpopmodder: Own one-way candidate advancement and operation-local rejection reasons.
public final class AutoDepositTrustedCandidateQueue {
    private final List<AutoDepositTrustedDestinationCandidate> candidates;
    private final Map<String, String> rejectedByCanonicalKey = new LinkedHashMap<>();
    private int index;

    public AutoDepositTrustedCandidateQueue(
            List<AutoDepositTrustedDestinationCandidate> candidates) {
        this.candidates = List.copyOf(candidates);
    }

    public Optional<AutoDepositTrustedDestinationCandidate> current() {
        skipRejected();
        return index < candidates.size() ? Optional.of(candidates.get(index)) : Optional.empty();
    }

    public void rejectCurrent(String reason) {
        current().ifPresent(candidate -> rejectedByCanonicalKey.put(
                candidate.destination().key(),
                reason == null ? "unspecified" : reason
        ));
        if (index < candidates.size()) {
            index++;
        }
        skipRejected();
    }

    public Map<String, String> rejected() {
        return Map.copyOf(rejectedByCanonicalKey);
    }

    public int remainingCandidateCount() {
        return Math.max(0, candidates.size() - index);
    }

    private void skipRejected() {
        while (index < candidates.size()
                && rejectedByCanonicalKey.containsKey(
                candidates.get(index).destination().key())) {
            index++;
        }
    }
}
