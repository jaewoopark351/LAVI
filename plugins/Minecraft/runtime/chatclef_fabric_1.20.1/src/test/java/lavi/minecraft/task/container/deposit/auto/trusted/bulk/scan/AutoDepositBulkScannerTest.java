package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Verify player-centered scanning without a container-anchor precondition.
class AutoDepositBulkScannerTest {
    private final AutoDepositBulkScanner scanner = new AutoDepositBulkScanner();

    @Test
    void scansExactly4096BasePositionsWithAnUnsupportedCenterAndCountsAllowlistedNeighbors() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.put(nonChest(
                new BlockPos(1, 64, 0),
                AutoDepositBulkContainerKind.BARREL
        ));
        world.put(chest(
                new BlockPos(2, 64, 0),
                AutoDepositBulkContainerKind.CHEST,
                AutoDepositBulkChestPart.SINGLE,
                Direction.NORTH,
                null
        ));
        world.put(nonChest(
                new BlockPos(3, 64, 0),
                AutoDepositBulkContainerKind.OTHER
        ));

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertTrue(result.success());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(4096, world.observationCalls);
        assertEquals(1, world.observationCount(anchor));
        assertEquals(2, result.physicalSupportedBlockCount());
        assertEquals(2, result.observations().stream()
                .filter(observation -> observation.discoveredInVolume()
                        && observation.kind().supported())
                .count());
        assertTrue(result.coverageComplete());
    }

    @Test
    void countsASupportedCenterExactlyOnceInTheNormalVolumeLoop() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.put(nonChest(anchor, AutoDepositBulkContainerKind.BARREL));

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertTrue(result.success());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(1, result.physicalSupportedBlockCount());
        assertEquals(1, world.observationCount(anchor));
    }

    @Test
    void clampsTheVerticalScanWithoutShiftingTheRequestedWindow() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(0, 70);

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertTrue(result.success());
        assertEquals(16 * 16 * 14, result.scannedPositionCount());
        AutoDepositBulkScanBounds bounds = result.bounds().orElseThrow();
        assertEquals(56, bounds.requestedMinY());
        assertEquals(72, bounds.requestedMaxYExclusive());
        assertEquals(56, bounds.effectiveMinY());
        assertEquals(70, bounds.effectiveMaxYExclusive());
    }

    @Test
    void rejectsAnUnloadedRequestedChunkBeforeReadingAnyBlock() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.unloadedChunks.add(chunkKey(-1, -1));

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals("requested_chunk_unloaded", result.reason());
        assertEquals(0, world.observationCalls);
        assertTrue(result.observations().isEmpty());
        assertFalse(result.coverageComplete());
    }

    @Test
    void rejectsAnUnloadedPartnerChunkWithoutPublishingTheLoadedSubset() {
        BlockPos anchor = new BlockPos(8, 64, 0);
        BlockPos insideHalf = new BlockPos(15, 64, 0);
        BlockPos outsidePartner = new BlockPos(16, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.put(chest(
                insideHalf,
                AutoDepositBulkContainerKind.CHEST,
                AutoDepositBulkChestPart.LEFT,
                Direction.NORTH,
                outsidePartner
        ));
        world.unloadedChunks.add(chunkKey(1, 0));

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals("double_chest_partner_chunk_unloaded", result.reason());
        assertTrue(result.observations().isEmpty());
        assertFalse(result.coverageComplete());
    }

    @Test
    void retainsLoadedPartnerEvidenceAcrossTheVolumeEdge() {
        BlockPos anchor = new BlockPos(8, 64, 0);
        BlockPos insideHalf = new BlockPos(15, 64, 0);
        BlockPos outsidePartner = new BlockPos(16, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.put(chest(
                insideHalf,
                AutoDepositBulkContainerKind.CHEST,
                AutoDepositBulkChestPart.LEFT,
                Direction.NORTH,
                outsidePartner
        ));
        world.put(chest(
                outsidePartner,
                AutoDepositBulkContainerKind.CHEST,
                AutoDepositBulkChestPart.RIGHT,
                Direction.NORTH,
                insideHalf
        ));

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertTrue(result.success());
        AutoDepositBulkBlockObservation partner = result.observations().stream()
                .filter(observation -> observation.position().equals(outsidePartner))
                .findFirst()
                .orElseThrow();
        assertFalse(partner.discoveredInVolume());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(1, result.physicalSupportedBlockCount());
        assertEquals(4097, result.observations().size());
        assertEquals(1, world.observationCount(insideHalf));
        assertEquals(1, world.observationCount(outsidePartner));
    }

    @Test
    void rejectsAWorldReplacementDetectedAfterTheScan() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.replaceIdentityOnSecondProvenance = true;

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals("world_provenance_changed_during_scan", result.reason());
        assertTrue(result.observations().isEmpty());
    }

    @Test
    void completesAnEmpty4096PositionVolumeWithoutInventingAContainer() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertTrue(result.success());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(0, result.physicalSupportedBlockCount());
        assertEquals(4096, result.observations().size());
        assertEquals(1, world.observationCount(anchor));
        assertTrue(result.coverageComplete());
    }

    @Test
    void rejectsUnavailableThrownAndMismatchedPreScanProvenanceBeforeOtherReads() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView unavailable = new FakeWorldView(-64, 320);
        unavailable.provenanceUnavailable = true;
        FakeWorldView provenanceFailure = new FakeWorldView(-64, 320);
        provenanceFailure.throwOnProvenance = true;
        FakeWorldView mismatch = new FakeWorldView(-64, 320);
        mismatch.replaceIdentityOnFirstProvenance = true;

        AutoDepositBulkScanResult unavailableResult = scanner.scan(
                unavailable,
                anchor,
                unavailable.expectedProvenance()
        );
        AutoDepositBulkScanResult thrownResult = scanner.scan(
                provenanceFailure,
                anchor,
                provenanceFailure.expectedProvenance()
        );
        AutoDepositBulkScanResult mismatchResult = scanner.scan(
                mismatch,
                anchor,
                mismatch.expectedProvenance()
        );

        assertPreScanFailure(unavailable, unavailableResult, "world_provenance_unavailable");
        assertPreScanFailure(
                provenanceFailure,
                thrownResult,
                "world_provenance_read_failed"
        );
        assertPreScanFailure(
                mismatch,
                mismatchResult,
                "world_provenance_changed_before_scan"
        );
    }

    @Test
    void failsClosedWhenBuildHeightReadThrows() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView buildHeightFailure = new FakeWorldView(-64, 320);
        buildHeightFailure.throwOnBuildHeight = true;

        AutoDepositBulkScanResult buildHeightResult = scanner.scan(
                buildHeightFailure,
                anchor,
                buildHeightFailure.expectedProvenance()
        );

        assertEquals("build_height_read_failed", buildHeightResult.reason());
        assertEquals(0, buildHeightResult.scannedPositionCount());
        assertTrue(buildHeightResult.observations().isEmpty());
    }

    @Test
    void preservesCompletedBaseCountsWhenABlockObservationThrows() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.put(nonChest(
                new BlockPos(-8, 56, -8),
                AutoDepositBulkContainerKind.BARREL
        ));
        world.throwOnObservationCall = 3;

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals("world_read_failed", result.reason());
        assertEquals(2, result.scannedPositionCount());
        assertEquals(1, result.physicalSupportedBlockCount());
        assertTrue(result.observations().isEmpty());
        assertFalse(result.coverageComplete());
    }

    @Test
    void preservesFullBaseCountsWhenPostScanProvenanceReadThrows() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.throwOnProvenanceCall = 2;

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals("world_read_failed", result.reason());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(0, result.physicalSupportedBlockCount());
        assertTrue(result.observations().isEmpty());
        assertFalse(result.coverageComplete());
    }

    @Test
    void rejectsAnAnchorOutsideBuildHeightBeforeChunkOrBlockReads() {
        BlockPos anchor = new BlockPos(0, -65, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.INVALID_ANCHOR, result.status());
        assertEquals("anchor_outside_build_height", result.reason());
        assertEquals(1, world.buildHeightCalls);
        assertEquals(0, world.chunkReadCalls);
        assertEquals(0, world.observationCalls);
        assertFalse(result.coverageComplete());
    }

    @Test
    void rejectsARequestedChunkThatUnloadsAfterTheFullScan() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        FakeWorldView world = new FakeWorldView(-64, 320);
        world.unloadRequestedChunkAfterFullScan = true;

        AutoDepositBulkScanResult result = scanner.scan(
                world,
                anchor,
                world.expectedProvenance()
        );

        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals("chunk_unloaded_during_scan", result.reason());
        assertEquals(4096, result.scannedPositionCount());
        assertTrue(result.observations().isEmpty());
        assertFalse(result.coverageComplete());
    }

    private static AutoDepositBulkBlockObservation nonChest(
            BlockPos position,
            AutoDepositBulkContainerKind kind) {
        return AutoDepositBulkBlockObservation.nonChest(position, kind, false);
    }

    private static AutoDepositBulkBlockObservation chest(
            BlockPos position,
            AutoDepositBulkContainerKind kind,
            AutoDepositBulkChestPart part,
            Direction facing,
            BlockPos partner) {
        return AutoDepositBulkBlockObservation.chest(
                position,
                kind,
                part,
                facing,
                partner,
                false
        );
    }

    private static String chunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }

    private static void assertPreScanFailure(
            FakeWorldView world,
            AutoDepositBulkScanResult result,
            String expectedReason) {
        assertEquals(AutoDepositBulkScanStatus.SCAN_COVERAGE_INCOMPLETE, result.status());
        assertEquals(expectedReason, result.reason());
        assertEquals(0, world.buildHeightCalls);
        assertEquals(0, world.chunkReadCalls);
        assertEquals(0, world.observationCalls);
        assertEquals(0, result.scannedPositionCount());
        assertEquals(0, result.physicalSupportedBlockCount());
        assertTrue(result.observations().isEmpty());
        assertFalse(result.coverageComplete());
    }

    private static final class FakeWorldView implements AutoDepositBulkWorldView {
        private final Object initialIdentity = new Object();
        private final Object replacementIdentity = new Object();
        private final AutoDepositBulkBuildHeight buildHeight;
        private final Map<BlockPos, AutoDepositBulkBlockObservation> observations =
                new HashMap<>();
        private final Map<BlockPos, Integer> observationCallsByPosition =
                new HashMap<>();
        private final Set<String> unloadedChunks = new HashSet<>();
        private int provenanceCalls;
        private int buildHeightCalls;
        private int chunkReadCalls;
        private int observationCalls;
        private boolean provenanceUnavailable;
        private boolean replaceIdentityOnFirstProvenance;
        private boolean replaceIdentityOnSecondProvenance;
        private boolean throwOnProvenance;
        private int throwOnProvenanceCall;
        private boolean throwOnBuildHeight;
        private int throwOnObservationCall;
        private boolean unloadRequestedChunkAfterFullScan;

        private FakeWorldView(int bottomY, int topYExclusive) {
            buildHeight = new AutoDepositBulkBuildHeight(bottomY, topYExclusive);
        }

        private void put(AutoDepositBulkBlockObservation observation) {
            observations.put(observation.position(), observation);
        }

        private AutoDepositBulkWorldProvenance expectedProvenance() {
            return provenance(initialIdentity);
        }

        private int observationCount(BlockPos position) {
            return observationCallsByPosition.getOrDefault(position, 0);
        }

        @Override
        public Optional<AutoDepositBulkWorldProvenance> provenance() {
            provenanceCalls++;
            if (throwOnProvenance
                    || throwOnProvenanceCall > 0
                    && provenanceCalls == throwOnProvenanceCall) {
                throw new IllegalStateException("expected provenance failure");
            }
            if (provenanceUnavailable) {
                return Optional.empty();
            }
            Object identity = replaceIdentityOnFirstProvenance
                    || replaceIdentityOnSecondProvenance && provenanceCalls > 1
                    ? replacementIdentity
                    : initialIdentity;
            return Optional.of(provenance(identity));
        }

        @Override
        public Optional<AutoDepositBulkBuildHeight> buildHeight() {
            buildHeightCalls++;
            if (throwOnBuildHeight) {
                throw new IllegalStateException("expected build-height failure");
            }
            return Optional.of(buildHeight);
        }

        @Override
        public boolean isChunkLoaded(int chunkX, int chunkZ) {
            chunkReadCalls++;
            if (unloadRequestedChunkAfterFullScan
                    && observationCalls >= 4096
                    && chunkX == 0
                    && chunkZ == 0) {
                return false;
            }
            return !unloadedChunks.contains(chunkKey(chunkX, chunkZ));
        }

        @Override
        public Optional<AutoDepositBulkBlockObservation> observeLoaded(BlockPos position) {
            observationCalls++;
            if (throwOnObservationCall > 0
                    && observationCalls == throwOnObservationCall) {
                throw new IllegalStateException("expected observation failure");
            }
            observationCallsByPosition.merge(position.toImmutable(), 1, Integer::sum);
            if (unloadedChunks.contains(chunkKey(
                    Math.floorDiv(position.getX(), 16),
                    Math.floorDiv(position.getZ(), 16)))) {
                return Optional.empty();
            }
            return Optional.of(observations.getOrDefault(
                    position,
                    AutoDepositBulkBlockObservation.nonChest(
                            position,
                            AutoDepositBulkContainerKind.OTHER,
                            false
                    )
            ));
        }

        private static AutoDepositBulkWorldProvenance provenance(Object worldIdentity) {
            return new AutoDepositBulkWorldProvenance(
                    "singleplayer:test",
                    Dimension.OVERWORLD,
                    "minecraft:overworld",
                    worldIdentity
            );
        }
    }
}
