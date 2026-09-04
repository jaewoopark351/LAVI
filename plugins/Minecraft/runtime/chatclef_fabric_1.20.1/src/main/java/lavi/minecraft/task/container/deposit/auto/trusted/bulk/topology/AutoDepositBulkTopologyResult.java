package lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkTopologyResult {
    private final AutoDepositBulkTopologyStatus status;
    private final String reason;
    private final String boundedFirstConflict;
    private final List<AutoDepositBulkLogicalDestination> logicalDestinations;
    private final int doubleChestCollapsedHalfCount;

    private AutoDepositBulkTopologyResult(
            AutoDepositBulkTopologyStatus status,
            String reason,
            String boundedFirstConflict,
            List<AutoDepositBulkLogicalDestination> logicalDestinations,
            int doubleChestCollapsedHalfCount) {
        this.status = Objects.requireNonNull(status, "status");
        this.reason = requireText(reason);
        this.boundedFirstConflict = requireText(boundedFirstConflict);
        this.logicalDestinations = Objects.requireNonNull(
                logicalDestinations,
                "logicalDestinations"
        ).stream().sorted(Comparator
                .comparingInt((AutoDepositBulkLogicalDestination destination) ->
                        destination.canonicalFirst().getX())
                .thenComparingInt(destination -> destination.canonicalFirst().getY())
                .thenComparingInt(destination -> destination.canonicalFirst().getZ()))
                .toList();
        if (doubleChestCollapsedHalfCount < 0) {
            throw new IllegalArgumentException(
                    "doubleChestCollapsedHalfCount must not be negative"
            );
        }
        this.doubleChestCollapsedHalfCount = doubleChestCollapsedHalfCount;
        if (status != AutoDepositBulkTopologyStatus.SUCCESS
                && !this.logicalDestinations.isEmpty()) {
            throw new IllegalArgumentException("failed topology result must not expose a partial set");
        }
    }

    static AutoDepositBulkTopologyResult success(
            List<AutoDepositBulkLogicalDestination> logicalDestinations,
            int doubleChestCollapsedHalfCount) {
        return new AutoDepositBulkTopologyResult(
                AutoDepositBulkTopologyStatus.SUCCESS,
                "complete",
                "none",
                logicalDestinations,
                doubleChestCollapsedHalfCount
        );
    }

    static AutoDepositBulkTopologyResult ambiguous(String reason, String firstConflict) {
        return new AutoDepositBulkTopologyResult(
                AutoDepositBulkTopologyStatus.AMBIGUOUS_DOUBLE_CHEST,
                reason,
                firstConflict,
                List.of(),
                0
        );
    }

    public AutoDepositBulkTopologyStatus status() {
        return status;
    }

    public boolean success() {
        return status == AutoDepositBulkTopologyStatus.SUCCESS;
    }

    public String reason() {
        return reason;
    }

    public String boundedFirstConflict() {
        return boundedFirstConflict;
    }

    public List<AutoDepositBulkLogicalDestination> logicalDestinations() {
        return logicalDestinations;
    }

    public int doubleChestCollapsedHalfCount() {
        return doubleChestCollapsedHalfCount;
    }

    private static String requireText(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("result text must not be blank");
        }
        return normalized;
    }
}
