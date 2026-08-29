package lavi.minecraft.diagnostics.container.home.timeout.state;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;

//20260829_kpopmodder: Own only diagnostic candidate catalog and ordinal bookkeeping.
public final class StoreHomeDiagnosticCandidateCatalogState {
    private int totalCandidateCount;
    private boolean catalogCaptured;
    private int nextCandidateOrdinal = 1;
    private int nextCandidateAttemptOrdinal = 1;
    private StoreHomeCandidateAttempt knownAttemptReference;
    private int knownCandidateOrdinal;
    private int knownCandidateAttemptOrdinal;

    public void captureCatalog(int candidateCount) {
        totalCandidateCount = Math.max(0, candidateCount);
        catalogCaptured = true;
    }

    public int totalCandidateCount() {
        return totalCandidateCount;
    }

    public boolean catalogCaptured() {
        return catalogCaptured;
    }

    public int effectiveCandidateCount(int observedRemaining) {
        return totalCandidateCount > 0
                ? totalCandidateCount
                : Math.max(0, observedRemaining);
    }

    public int reserveCandidateOrdinal(int remainingIncludingCurrent) {
        int remaining = Math.max(0, remainingIncludingCurrent);
        int inferred = catalogCaptured
                && remaining > 0
                && remaining <= totalCandidateCount
                ? totalCandidateCount - remaining + 1
                : nextCandidateOrdinal;
        int ordinal = Math.max(1, Math.max(nextCandidateOrdinal, inferred));
        nextCandidateOrdinal = ordinal < Integer.MAX_VALUE
                ? ordinal + 1
                : Integer.MAX_VALUE;
        return ordinal;
    }

    public void rememberAttempt(
            StoreHomeCandidateAttempt attempt,
            int remainingIncludingCurrent) {
        if (attempt == null || knownAttemptReference == attempt) {
            return;
        }
        knownAttemptReference = attempt;
        knownCandidateOrdinal = reserveCandidateOrdinal(remainingIncludingCurrent);
        knownCandidateAttemptOrdinal = nextCandidateAttemptOrdinal;
        if (nextCandidateAttemptOrdinal < Integer.MAX_VALUE) {
            nextCandidateAttemptOrdinal++;
        }
    }

    public void clearKnownAttempt() {
        knownAttemptReference = null;
        knownCandidateOrdinal = 0;
        knownCandidateAttemptOrdinal = 0;
    }

    public StoreHomeCandidateAttempt knownAttemptReference() {
        return knownAttemptReference;
    }

    public boolean knownCandidateMatches(
            AutoDepositTrustedDestinationCandidate candidate) {
        return knownAttemptReference != null
                && knownAttemptReference.candidate() == candidate;
    }

    public int knownCandidateOrdinal() {
        return knownCandidateOrdinal;
    }

    public int knownCandidateAttemptOrdinal() {
        return knownCandidateAttemptOrdinal;
    }
}
