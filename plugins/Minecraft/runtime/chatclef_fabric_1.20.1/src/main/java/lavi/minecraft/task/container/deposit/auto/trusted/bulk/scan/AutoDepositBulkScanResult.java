package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkScanResult {
    private final AutoDepositBulkScanStatus status;
    private final String reason;
    private final String boundedFirstConflict;
    private final BlockPos anchorPosition;
    private final AutoDepositBulkWorldProvenance provenance;
    private final AutoDepositBulkScanBounds bounds;
    private final List<AutoDepositBulkBlockObservation> observations;
    private final int scannedPositionCount;
    private final int physicalSupportedBlockCount;
    private final boolean coverageComplete;

    private AutoDepositBulkScanResult(
            AutoDepositBulkScanStatus status,
            String reason,
            String boundedFirstConflict,
            BlockPos anchorPosition,
            AutoDepositBulkWorldProvenance provenance,
            AutoDepositBulkScanBounds bounds,
            List<AutoDepositBulkBlockObservation> observations,
            int scannedPositionCount,
            int physicalSupportedBlockCount,
            boolean coverageComplete) {
        this.status = Objects.requireNonNull(status, "status");
        this.reason = requireReason(reason);
        this.boundedFirstConflict = requireReason(boundedFirstConflict);
        this.anchorPosition = Objects.requireNonNull(anchorPosition, "anchorPosition")
                .toImmutable();
        this.provenance = provenance;
        this.bounds = bounds;
        this.observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
        this.scannedPositionCount = requireNonNegative(
                scannedPositionCount,
                "scannedPositionCount"
        );
        this.physicalSupportedBlockCount = requireNonNegative(
                physicalSupportedBlockCount,
                "physicalSupportedBlockCount"
        );
        this.coverageComplete = coverageComplete;
        if (status == AutoDepositBulkScanStatus.SUCCESS
                && (!coverageComplete || provenance == null || bounds == null)) {
            throw new IllegalArgumentException(
                    "successful scan requires complete coverage, provenance, and bounds"
            );
        }
    }

    public static AutoDepositBulkScanResult success(
            BlockPos anchorPosition,
            AutoDepositBulkWorldProvenance provenance,
            AutoDepositBulkScanBounds bounds,
            List<AutoDepositBulkBlockObservation> observations,
            int scannedPositionCount,
            int physicalSupportedBlockCount) {
        return new AutoDepositBulkScanResult(
                AutoDepositBulkScanStatus.SUCCESS,
                "complete",
                "none",
                anchorPosition,
                provenance,
                bounds,
                observations,
                scannedPositionCount,
                physicalSupportedBlockCount,
                true
        );
    }

    static AutoDepositBulkScanResult failure(
            AutoDepositBulkScanStatus status,
            String reason,
            String boundedFirstConflict,
            BlockPos anchorPosition,
            AutoDepositBulkWorldProvenance provenance,
            AutoDepositBulkScanBounds bounds,
            int scannedPositionCount,
            int physicalSupportedBlockCount) {
        if (status == AutoDepositBulkScanStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status must not be SUCCESS");
        }
        return new AutoDepositBulkScanResult(
                status,
                reason,
                boundedFirstConflict,
                anchorPosition,
                provenance,
                bounds,
                List.of(),
                scannedPositionCount,
                physicalSupportedBlockCount,
                false
        );
    }

    public AutoDepositBulkScanStatus status() {
        return status;
    }

    public boolean success() {
        return status == AutoDepositBulkScanStatus.SUCCESS;
    }

    public String reason() {
        return reason;
    }

    public String boundedFirstConflict() {
        return boundedFirstConflict;
    }

    public BlockPos anchorPosition() {
        return anchorPosition;
    }

    public Optional<AutoDepositBulkWorldProvenance> provenance() {
        return Optional.ofNullable(provenance);
    }

    public Optional<AutoDepositBulkScanBounds> bounds() {
        return Optional.ofNullable(bounds);
    }

    public List<AutoDepositBulkBlockObservation> observations() {
        return observations;
    }

    public int scannedPositionCount() {
        return scannedPositionCount;
    }

    public int physicalSupportedBlockCount() {
        return physicalSupportedBlockCount;
    }

    public boolean coverageComplete() {
        return coverageComplete;
    }

    private static String requireReason(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("result text must not be blank");
        }
        return normalized;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
