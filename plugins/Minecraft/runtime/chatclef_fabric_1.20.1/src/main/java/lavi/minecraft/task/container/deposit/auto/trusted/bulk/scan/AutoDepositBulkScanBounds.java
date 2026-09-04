package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public record AutoDepositBulkScanBounds(
        int requestedMinX,
        int requestedMaxXExclusive,
        int requestedMinY,
        int requestedMaxYExclusive,
        int requestedMinZ,
        int requestedMaxZExclusive,
        int effectiveMinY,
        int effectiveMaxYExclusive) {
    public static final int SIDE_LENGTH = 16;
    public static final int HALF_LENGTH = 8;
    public static final int MAX_BASE_POSITION_COUNT = SIDE_LENGTH * SIDE_LENGTH * SIDE_LENGTH;

    public AutoDepositBulkScanBounds {
        if (requestedMaxXExclusive - requestedMinX != SIDE_LENGTH
                || requestedMaxYExclusive - requestedMinY != SIDE_LENGTH
                || requestedMaxZExclusive - requestedMinZ != SIDE_LENGTH) {
            throw new IllegalArgumentException("requested H5 bounds must be exactly 16x16x16");
        }
        if (effectiveMinY < requestedMinY
                || effectiveMaxYExclusive > requestedMaxYExclusive
                || effectiveMaxYExclusive <= effectiveMinY) {
            throw new IllegalArgumentException("effective Y bounds must be a non-empty subset");
        }
    }

    public static AutoDepositBulkScanBounds around(
            BlockPos anchor,
            AutoDepositBulkBuildHeight buildHeight) {
        BlockPos checkedAnchor = Objects.requireNonNull(anchor, "anchor");
        AutoDepositBulkBuildHeight checkedHeight = Objects.requireNonNull(
                buildHeight,
                "buildHeight"
        );
        if (checkedAnchor.getY() < checkedHeight.bottomY()
                || checkedAnchor.getY() >= checkedHeight.topYExclusive()) {
            throw new IllegalArgumentException("anchor Y must be inside world build height");
        }
        int minX = checkedAnchor.getX() - HALF_LENGTH;
        int minY = checkedAnchor.getY() - HALF_LENGTH;
        int minZ = checkedAnchor.getZ() - HALF_LENGTH;
        int maxX = checkedAnchor.getX() + HALF_LENGTH;
        int maxY = checkedAnchor.getY() + HALF_LENGTH;
        int maxZ = checkedAnchor.getZ() + HALF_LENGTH;
        int effectiveMinY = Math.max(minY, checkedHeight.bottomY());
        int effectiveMaxY = Math.min(maxY, checkedHeight.topYExclusive());
        return new AutoDepositBulkScanBounds(
                minX,
                maxX,
                minY,
                maxY,
                minZ,
                maxZ,
                effectiveMinY,
                effectiveMaxY
        );
    }

    public int effectivePositionCount() {
        return SIDE_LENGTH * SIDE_LENGTH * (effectiveMaxYExclusive - effectiveMinY);
    }

    public boolean containsEffective(BlockPos position) {
        BlockPos checked = Objects.requireNonNull(position, "position");
        return checked.getX() >= requestedMinX
                && checked.getX() < requestedMaxXExclusive
                && checked.getY() >= effectiveMinY
                && checked.getY() < effectiveMaxYExclusive
                && checked.getZ() >= requestedMinZ
                && checked.getZ() < requestedMaxZExclusive;
    }
}
