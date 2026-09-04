package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkScanner {
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
            .comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ);

    public AutoDepositBulkScanResult scan(
            AutoDepositBulkWorldView worldView,
            BlockPos anchorPosition,
            AutoDepositBulkWorldProvenance expectedProvenance) {
        AutoDepositBulkWorldView checkedWorld = Objects.requireNonNull(
                worldView,
                "worldView"
        );
        BlockPos anchor = Objects.requireNonNull(anchorPosition, "anchorPosition")
                .toImmutable();
        AutoDepositBulkWorldProvenance expected = Objects.requireNonNull(
                expectedProvenance,
                "expectedProvenance"
        );
        Optional<AutoDepositBulkWorldProvenance> beforeOptional;
        int scannedPositionCount = 0;
        int physicalSupportedBlockCount = 0;
        try {
            beforeOptional = Objects.requireNonNull(
                    checkedWorld.provenance(),
                    "worldView.provenance()"
            );
        } catch (RuntimeException exception) {
            return failure(
                    "world_provenance_read_failed",
                    "exception=" + exception.getClass().getSimpleName(),
                    anchor,
                    null,
                    null,
                    0,
                    0
            );
        }
        if (beforeOptional.isEmpty()) {
            return failure(
                    "world_provenance_unavailable",
                    "provenance=unavailable",
                    anchor,
                    null,
                    null,
                    0,
                    0
            );
        }
        AutoDepositBulkWorldProvenance before = beforeOptional.get();
        if (!expected.sameWorld(before)) {
            return failure(
                    "world_provenance_changed_before_scan",
                    "provenance=changed",
                    anchor,
                    before,
                    null,
                    0,
                    0
            );
        }
        Optional<AutoDepositBulkBuildHeight> buildHeightOptional;
        try {
            buildHeightOptional = Objects.requireNonNull(
                    checkedWorld.buildHeight(),
                    "worldView.buildHeight()"
            );
        } catch (RuntimeException exception) {
            return failure(
                    "build_height_read_failed",
                    "exception=" + exception.getClass().getSimpleName(),
                    anchor,
                    before,
                    null,
                    0,
                    0
            );
        }
        if (buildHeightOptional.isEmpty()) {
            return failure(
                    "build_height_unavailable",
                    "buildHeight=unavailable",
                    anchor,
                    before,
                    null,
                    0,
                    0
            );
        }

        AutoDepositBulkScanBounds bounds;
        try {
            bounds = AutoDepositBulkScanBounds.around(anchor, buildHeightOptional.get());
        } catch (IllegalArgumentException exception) {
            return AutoDepositBulkScanResult.failure(
                    AutoDepositBulkScanStatus.INVALID_ANCHOR,
                    "anchor_outside_build_height",
                    "anchor=" + anchor.toShortString(),
                    anchor,
                    before,
                    null,
                    0,
                    0
            );
        }

        try {
            Optional<String> missingRequestedChunk = firstMissingRequestedChunk(
                    checkedWorld,
                    bounds
            );
            if (missingRequestedChunk.isPresent()) {
                return failure(
                        "requested_chunk_unloaded",
                        missingRequestedChunk.get(),
                        anchor,
                        before,
                        bounds,
                        0,
                        0
                );
            }

            Map<BlockPos, AutoDepositBulkBlockObservation> observations =
                    new LinkedHashMap<>();
            Set<BlockPos> partnerPositions = new LinkedHashSet<>();
            for (int x = bounds.requestedMinX(); x < bounds.requestedMaxXExclusive(); x++) {
                for (int y = bounds.effectiveMinY(); y < bounds.effectiveMaxYExclusive(); y++) {
                    for (int z = bounds.requestedMinZ(); z < bounds.requestedMaxZExclusive(); z++) {
                        BlockPos position = new BlockPos(x, y, z);
                        Optional<AutoDepositBulkBlockObservation> observedOptional =
                                checkedWorld.observeLoaded(position).map(
                                        observed -> observed.withDiscoveredInVolume(true)
                                );
                        if (observedOptional.isEmpty()) {
                            return failure(
                                    "block_observation_unavailable",
                                    "pos=" + position.toShortString(),
                                    anchor,
                                    before,
                                    bounds,
                                    scannedPositionCount,
                                    physicalSupportedBlockCount
                            );
                        }
                        AutoDepositBulkBlockObservation observed = observedOptional.get();
                        observations.put(observed.position(), observed);
                        scannedPositionCount++;
                        if (observed.kind().supported()) {
                            physicalSupportedBlockCount++;
                        }
                        if (observed.kind().chest() && observed.chestPart().doubleHalf()) {
                            observed.partnerPosition().ifPresent(partnerPositions::add);
                        }
                    }
                }
            }

            List<BlockPos> sortedPartners = new ArrayList<>(partnerPositions);
            sortedPartners.sort(POSITION_ORDER);
            for (BlockPos partnerPosition : sortedPartners) {
                int chunkX = Math.floorDiv(partnerPosition.getX(), 16);
                int chunkZ = Math.floorDiv(partnerPosition.getZ(), 16);
                if (!checkedWorld.isChunkLoaded(chunkX, chunkZ)) {
                    return failure(
                            "double_chest_partner_chunk_unloaded",
                            "chunk=" + chunkX + "," + chunkZ,
                            anchor,
                            before,
                            bounds,
                            scannedPositionCount,
                            physicalSupportedBlockCount
                    );
                }
                if (!observations.containsKey(partnerPosition)) {
                    Optional<AutoDepositBulkBlockObservation> partner =
                            checkedWorld.observeLoaded(partnerPosition);
                    if (partner.isEmpty()) {
                        return failure(
                                "double_chest_partner_observation_unavailable",
                                "pos=" + partnerPosition.toShortString(),
                                anchor,
                                before,
                                bounds,
                                scannedPositionCount,
                                physicalSupportedBlockCount
                        );
                    }
                    observations.put(
                            partnerPosition,
                            partner.get().withDiscoveredInVolume(false)
                    );
                }
            }

            Optional<String> missingAfterScan = firstMissingRequiredChunk(
                    checkedWorld,
                    bounds,
                    sortedPartners
            );
            if (missingAfterScan.isPresent()) {
                return failure(
                        "chunk_unloaded_during_scan",
                        missingAfterScan.get(),
                        anchor,
                        before,
                        bounds,
                        scannedPositionCount,
                        physicalSupportedBlockCount
                );
            }
            Optional<AutoDepositBulkWorldProvenance> after = checkedWorld.provenance();
            if (after.isEmpty() || !before.sameWorld(after.get())) {
                return failure(
                        "world_provenance_changed_during_scan",
                        "provenance=changed",
                        anchor,
                        before,
                        bounds,
                        scannedPositionCount,
                        physicalSupportedBlockCount
                );
            }

            return AutoDepositBulkScanResult.success(
                    anchor,
                    before,
                    bounds,
                    List.copyOf(observations.values()),
                    scannedPositionCount,
                    physicalSupportedBlockCount
            );
        } catch (RuntimeException exception) {
            return failure(
                    "world_read_failed",
                    "exception=" + exception.getClass().getSimpleName(),
                anchor,
                before,
                bounds,
                scannedPositionCount,
                physicalSupportedBlockCount
            );
        }
    }

    private static Optional<String> firstMissingRequestedChunk(
            AutoDepositBulkWorldView worldView,
            AutoDepositBulkScanBounds bounds) {
        int minChunkX = Math.floorDiv(bounds.requestedMinX(), 16);
        int maxChunkX = Math.floorDiv(bounds.requestedMaxXExclusive() - 1, 16);
        int minChunkZ = Math.floorDiv(bounds.requestedMinZ(), 16);
        int maxChunkZ = Math.floorDiv(bounds.requestedMaxZExclusive() - 1, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!worldView.isChunkLoaded(chunkX, chunkZ)) {
                    return Optional.of("chunk=" + chunkX + "," + chunkZ);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> firstMissingRequiredChunk(
            AutoDepositBulkWorldView worldView,
            AutoDepositBulkScanBounds bounds,
            List<BlockPos> partnerPositions) {
        Optional<String> requestedMissing = firstMissingRequestedChunk(worldView, bounds);
        if (requestedMissing.isPresent()) {
            return requestedMissing;
        }
        for (BlockPos partnerPosition : partnerPositions) {
            int chunkX = Math.floorDiv(partnerPosition.getX(), 16);
            int chunkZ = Math.floorDiv(partnerPosition.getZ(), 16);
            if (!worldView.isChunkLoaded(chunkX, chunkZ)) {
                return Optional.of("chunk=" + chunkX + "," + chunkZ);
            }
        }
        return Optional.empty();
    }

    private static AutoDepositBulkScanResult failure(
            String reason,
            String boundedFirstConflict,
            BlockPos anchor,
            AutoDepositBulkWorldProvenance provenance,
            AutoDepositBulkScanBounds bounds,
            int scannedPositionCount,
            int physicalSupportedBlockCount) {
        return AutoDepositBulkScanResult.failure(
                AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE,
                reason,
                boundedFirstConflict,
                anchor,
                provenance,
                bounds,
                scannedPositionCount,
                physicalSupportedBlockCount
        );
    }
}
