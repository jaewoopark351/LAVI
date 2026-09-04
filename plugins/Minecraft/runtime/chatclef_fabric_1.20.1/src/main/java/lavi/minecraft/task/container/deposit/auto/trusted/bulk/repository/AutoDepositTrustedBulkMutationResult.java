package lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository;

import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadStatus;

import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedBulkMutationResult {
    private final AutoDepositTrustedBulkMutationStatus status;
    private final String reason;
    private final String firstConflict;
    private final AutoDepositTrustedRegistryReadStatus registryReadStatus;
    private final int newlyRegisteredCount;
    private final int reenabledCount;
    private final int alreadyRegisteredCount;
    private final long repositoryRevisionBefore;
    private final long repositoryRevisionAfter;
    private final int totalRegistryCountBefore;
    private final int totalRegistryCountAfter;

    private AutoDepositTrustedBulkMutationResult(
            AutoDepositTrustedBulkMutationStatus status,
            String reason,
            String firstConflict,
            AutoDepositTrustedRegistryReadStatus registryReadStatus,
            int newlyRegisteredCount,
            int reenabledCount,
            int alreadyRegisteredCount,
            long repositoryRevisionBefore,
            long repositoryRevisionAfter,
            int totalRegistryCountBefore,
            int totalRegistryCountAfter) {
        this.status = Objects.requireNonNull(status, "status");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.firstConflict = firstConflict == null ? "" : firstConflict;
        this.registryReadStatus = Objects.requireNonNull(registryReadStatus, "registryReadStatus");
        this.newlyRegisteredCount = newlyRegisteredCount;
        this.reenabledCount = reenabledCount;
        this.alreadyRegisteredCount = alreadyRegisteredCount;
        this.repositoryRevisionBefore = repositoryRevisionBefore;
        this.repositoryRevisionAfter = repositoryRevisionAfter;
        this.totalRegistryCountBefore = totalRegistryCountBefore;
        this.totalRegistryCountAfter = totalRegistryCountAfter;
    }

    public static AutoDepositTrustedBulkMutationResult of(
            AutoDepositTrustedBulkMutationStatus status,
            String reason,
            String firstConflict,
            AutoDepositTrustedRegistryReadStatus registryReadStatus,
            AutoDepositTrustedBulkMergePlan plan,
            long repositoryRevisionBefore,
            long repositoryRevisionAfter,
            int totalRegistryCountBefore,
            int totalRegistryCountAfter) {
        int newlyRegisteredCount = plan == null ? 0 : plan.newlyRegisteredCount();
        int reenabledCount = plan == null ? 0 : plan.reenabledCount();
        int alreadyRegisteredCount = plan == null ? 0 : plan.alreadyRegisteredCount();
        return new AutoDepositTrustedBulkMutationResult(
                status,
                reason,
                firstConflict,
                registryReadStatus,
                newlyRegisteredCount,
                reenabledCount,
                alreadyRegisteredCount,
                repositoryRevisionBefore,
                repositoryRevisionAfter,
                totalRegistryCountBefore,
                totalRegistryCountAfter
        );
    }

    public AutoDepositTrustedBulkMutationStatus status() {
        return status;
    }

    public boolean success() {
        return status.success();
    }

    public boolean effectiveMutation() {
        return status == AutoDepositTrustedBulkMutationStatus.UPDATED;
    }

    public String reason() {
        return reason;
    }

    public String firstConflict() {
        return firstConflict;
    }

    public AutoDepositTrustedRegistryReadStatus registryReadStatus() {
        return registryReadStatus;
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

    public long repositoryRevisionBefore() {
        return repositoryRevisionBefore;
    }

    public long repositoryRevisionAfter() {
        return repositoryRevisionAfter;
    }

    public int totalRegistryCountBefore() {
        return totalRegistryCountBefore;
    }

    public int totalRegistryCountAfter() {
        return totalRegistryCountAfter;
    }
}
