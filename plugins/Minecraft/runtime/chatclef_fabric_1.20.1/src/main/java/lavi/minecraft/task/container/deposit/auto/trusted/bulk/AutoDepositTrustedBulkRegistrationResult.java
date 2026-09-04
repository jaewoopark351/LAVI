package lavi.minecraft.task.container.deposit.auto.trusted.bulk;

import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanBounds;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkTopologyResult;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedBulkRegistrationResult {
    private final boolean success;
    private final String status;
    private final String reason;
    private final String boundedFirstConflict;
    private final AutoDepositTrustCommandForm commandForm;
    private final String registryReadStatus;
    private final AutoDepositBulkAnchorKind anchorSource;
    private final BlockPos anchorPosition;
    private final AutoDepositBulkScanBounds bounds;
    private final boolean coverageComplete;
    private final int scannedPositionCount;
    private final int physicalSupportedBlockCount;
    private final int logicalDestinationCount;
    private final int newlyRegisteredCount;
    private final int reenabledCount;
    private final int alreadyRegisteredCount;
    private final int doubleChestCollapsedHalfCount;
    private final long repositoryRevisionBefore;
    private final long repositoryRevisionAfter;
    private final int totalRegistryCountBefore;
    private final int totalRegistryCountAfter;
    private final boolean downstreamAutomaticReevaluationPossible;

    private AutoDepositTrustedBulkRegistrationResult(
            boolean success,
            String status,
            String reason,
            String boundedFirstConflict,
            AutoDepositTrustCommandForm commandForm,
            String registryReadStatus,
            AutoDepositBulkAnchorKind anchorSource,
            BlockPos anchorPosition,
            AutoDepositBulkScanBounds bounds,
            boolean coverageComplete,
            int scannedPositionCount,
            int physicalSupportedBlockCount,
            int logicalDestinationCount,
            int newlyRegisteredCount,
            int reenabledCount,
            int alreadyRegisteredCount,
            int doubleChestCollapsedHalfCount,
            long repositoryRevisionBefore,
            long repositoryRevisionAfter,
            int totalRegistryCountBefore,
            int totalRegistryCountAfter,
            boolean downstreamAutomaticReevaluationPossible) {
        this.success = success;
        this.status = requireText(status, "status");
        this.reason = requireText(reason, "reason");
        this.boundedFirstConflict = bounded(boundedFirstConflict);
        this.commandForm = Objects.requireNonNull(commandForm, "commandForm");
        this.registryReadStatus = requireText(registryReadStatus, "registryReadStatus");
        this.anchorSource = Objects.requireNonNull(anchorSource, "anchorSource");
        this.anchorPosition = anchorPosition == null ? null : anchorPosition.toImmutable();
        this.bounds = bounds;
        this.coverageComplete = coverageComplete;
        this.scannedPositionCount = scannedPositionCount;
        this.physicalSupportedBlockCount = physicalSupportedBlockCount;
        this.logicalDestinationCount = logicalDestinationCount;
        this.newlyRegisteredCount = newlyRegisteredCount;
        this.reenabledCount = reenabledCount;
        this.alreadyRegisteredCount = alreadyRegisteredCount;
        this.doubleChestCollapsedHalfCount = doubleChestCollapsedHalfCount;
        this.repositoryRevisionBefore = repositoryRevisionBefore;
        this.repositoryRevisionAfter = repositoryRevisionAfter;
        this.totalRegistryCountBefore = totalRegistryCountBefore;
        this.totalRegistryCountAfter = totalRegistryCountAfter;
        this.downstreamAutomaticReevaluationPossible = downstreamAutomaticReevaluationPossible;
    }

    public static AutoDepositTrustedBulkRegistrationResult beforeScanFailure(
            AutoDepositBulkAnchorKind anchorSource,
            AutoDepositTrustCommandForm form,
            BlockPos anchor,
            String status,
            String reason,
            String conflict) {
        return base(false, status, reason, conflict, form, "NOT_READ", anchorSource,
                anchor, null,
                false, 0, 0, 0, 0, 0, 0, 0,
                -1L, -1L, -1, -1, false);
    }

    public static AutoDepositTrustedBulkRegistrationResult scanFailure(
            AutoDepositBulkAnchorKind anchorSource,
            AutoDepositTrustCommandForm form,
            BlockPos anchor,
            AutoDepositBulkScanResult scan) {
        Objects.requireNonNull(scan, "scan");
        return base(false, scan.status().name(), scan.reason(),
                scan.boundedFirstConflict(), form, "NOT_READ", anchorSource, anchor,
                scan.bounds().orElse(null), scan.coverageComplete(),
                scan.scannedPositionCount(), scan.physicalSupportedBlockCount(),
                0, 0, 0, 0, 0, -1L, -1L, -1, -1, false);
    }

    public static AutoDepositTrustedBulkRegistrationResult topologyFailure(
            AutoDepositBulkAnchorKind anchorSource,
            AutoDepositTrustCommandForm form,
            AutoDepositBulkScanResult scan,
            AutoDepositBulkTopologyResult topology) {
        Objects.requireNonNull(scan, "scan");
        Objects.requireNonNull(topology, "topology");
        return base(false, topology.status().name(), topology.reason(),
                topology.boundedFirstConflict(), form, "NOT_READ", anchorSource,
                scan.anchorPosition(), scan.bounds().orElse(null),
                scan.coverageComplete(), scan.scannedPositionCount(),
                scan.physicalSupportedBlockCount(), 0, 0, 0, 0,
                topology.doubleChestCollapsedHalfCount(),
                -1L, -1L, -1, -1, false);
    }

    public static AutoDepositTrustedBulkRegistrationResult postScanFailure(
            AutoDepositBulkAnchorKind anchorSource,
            AutoDepositTrustCommandForm form,
            AutoDepositBulkScanResult scan,
            AutoDepositBulkTopologyResult topology,
            String status,
            String reason,
            String conflict) {
        Objects.requireNonNull(scan, "scan");
        int logicalDestinationCount = topology == null
                ? 0
                : topology.logicalDestinations().size();
        int collapsedHalfCount = topology == null
                ? 0
                : topology.doubleChestCollapsedHalfCount();
        return base(false, status, reason, conflict, form, "NOT_READ", anchorSource,
                scan.anchorPosition(), scan.bounds().orElse(null),
                scan.coverageComplete(), scan.scannedPositionCount(),
                scan.physicalSupportedBlockCount(), logicalDestinationCount,
                0, 0, 0, collapsedHalfCount,
                -1L, -1L, -1, -1, false);
    }

    public static AutoDepositTrustedBulkRegistrationResult mutation(
            AutoDepositBulkAnchorKind anchorSource,
            AutoDepositTrustCommandForm form,
            AutoDepositBulkScanResult scan,
            AutoDepositBulkTopologyResult topology,
            AutoDepositTrustedBulkMutationResult mutation) {
        Objects.requireNonNull(scan, "scan");
        Objects.requireNonNull(topology, "topology");
        Objects.requireNonNull(mutation, "mutation");
        return base(mutation.success(), mutation.status().name(), mutation.reason(),
                mutation.firstConflict(), form, mutation.registryReadStatus().name(),
                anchorSource,
                scan.anchorPosition(), scan.bounds().orElse(null),
                scan.coverageComplete(), scan.scannedPositionCount(),
                scan.physicalSupportedBlockCount(), topology.logicalDestinations().size(),
                mutation.newlyRegisteredCount(), mutation.reenabledCount(),
                mutation.alreadyRegisteredCount(), topology.doubleChestCollapsedHalfCount(),
                mutation.repositoryRevisionBefore(), mutation.repositoryRevisionAfter(),
                mutation.totalRegistryCountBefore(), mutation.totalRegistryCountAfter(),
                mutation.effectiveMutation());
    }

    private static AutoDepositTrustedBulkRegistrationResult base(
            boolean success,
            String status,
            String reason,
            String conflict,
            AutoDepositTrustCommandForm form,
            String readStatus,
            AutoDepositBulkAnchorKind anchorSource,
            BlockPos anchor,
            AutoDepositBulkScanBounds bounds,
            boolean coverageComplete,
            int scannedPositionCount,
            int physicalSupportedBlockCount,
            int logicalDestinationCount,
            int newlyRegisteredCount,
            int reenabledCount,
            int alreadyRegisteredCount,
            int doubleChestCollapsedHalfCount,
            long revisionBefore,
            long revisionAfter,
            int totalBefore,
            int totalAfter,
            boolean downstreamPossible) {
        return new AutoDepositTrustedBulkRegistrationResult(
                success, status, reason, conflict, form, readStatus, anchorSource,
                anchor, bounds,
                coverageComplete, scannedPositionCount, physicalSupportedBlockCount,
                logicalDestinationCount, newlyRegisteredCount, reenabledCount,
                alreadyRegisteredCount, doubleChestCollapsedHalfCount, revisionBefore,
                revisionAfter, totalBefore, totalAfter, downstreamPossible
        );
    }

    public boolean success() { return success; }
    public String status() { return status; }
    public String reason() { return reason; }
    public String boundedFirstConflict() { return boundedFirstConflict; }
    public AutoDepositTrustCommandForm commandForm() { return commandForm; }
    public String registryReadStatus() { return registryReadStatus; }
    public AutoDepositBulkAnchorKind anchorSource() { return anchorSource; }
    public Optional<BlockPos> anchorPosition() { return Optional.ofNullable(anchorPosition); }
    public Optional<AutoDepositBulkScanBounds> bounds() { return Optional.ofNullable(bounds); }
    public boolean coverageComplete() { return coverageComplete; }
    public int scannedPositionCount() { return scannedPositionCount; }
    public int physicalSupportedBlockCount() { return physicalSupportedBlockCount; }
    public int logicalDestinationCount() { return logicalDestinationCount; }
    public int newlyRegisteredCount() { return newlyRegisteredCount; }
    public int reenabledCount() { return reenabledCount; }
    public int alreadyRegisteredCount() { return alreadyRegisteredCount; }
    public int doubleChestCollapsedHalfCount() { return doubleChestCollapsedHalfCount; }
    public long repositoryRevisionBefore() { return repositoryRevisionBefore; }
    public long repositoryRevisionAfter() { return repositoryRevisionAfter; }
    public int totalRegistryCountBefore() { return totalRegistryCountBefore; }
    public int totalRegistryCountAfter() { return totalRegistryCountAfter; }
    public boolean downstreamAutomaticReevaluationPossible() {
        return downstreamAutomaticReevaluationPossible;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String bounded(String value) {
        String normalized = value == null || value.isBlank() ? "none" : value.trim();
        return normalized.length() <= 256 ? normalized : normalized.substring(0, 256);
    }
}
