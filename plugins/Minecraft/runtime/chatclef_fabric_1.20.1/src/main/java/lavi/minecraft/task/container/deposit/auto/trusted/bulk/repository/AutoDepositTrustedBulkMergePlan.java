package lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;

import java.util.List;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedBulkMergePlan {
    private final AutoDepositTrustedBulkMergeStatus status;
    private final List<AutoDepositTrustedDestination> finalDestinations;
    private final int newlyRegisteredCount;
    private final int reenabledCount;
    private final int alreadyRegisteredCount;
    private final String firstConflict;

    private AutoDepositTrustedBulkMergePlan(
            AutoDepositTrustedBulkMergeStatus status,
            List<AutoDepositTrustedDestination> finalDestinations,
            int newlyRegisteredCount,
            int reenabledCount,
            int alreadyRegisteredCount,
            String firstConflict) {
        this.status = Objects.requireNonNull(status, "status");
        this.finalDestinations = List.copyOf(
                Objects.requireNonNull(finalDestinations, "finalDestinations")
        );
        this.newlyRegisteredCount = newlyRegisteredCount;
        this.reenabledCount = reenabledCount;
        this.alreadyRegisteredCount = alreadyRegisteredCount;
        this.firstConflict = firstConflict == null ? "" : firstConflict;
    }

    public static AutoDepositTrustedBulkMergePlan of(
            AutoDepositTrustedBulkMergeStatus status,
            List<AutoDepositTrustedDestination> finalDestinations,
            int newlyRegisteredCount,
            int reenabledCount,
            int alreadyRegisteredCount,
            String firstConflict) {
        return new AutoDepositTrustedBulkMergePlan(
                status,
                finalDestinations,
                newlyRegisteredCount,
                reenabledCount,
                alreadyRegisteredCount,
                firstConflict
        );
    }

    public AutoDepositTrustedBulkMergeStatus status() {
        return status;
    }

    public List<AutoDepositTrustedDestination> finalDestinations() {
        return finalDestinations;
    }

    public int newlyRegisteredCount() {
        return newlyRegisteredCount;
    }

    public int reenabledCount() {
        return reenabledCount;
    }

    public int alreadyRegisteredCount() {
        return alreadyRegisteredCount;
    }

    public String firstConflict() {
        return firstConflict;
    }

    public boolean mutationRequired() {
        return status == AutoDepositTrustedBulkMergeStatus.MUTATION_REQUIRED;
    }
}
