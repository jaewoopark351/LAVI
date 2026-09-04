package lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkBlockObservation;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkBuildHeight;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkChestPart;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkContainerKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanBounds;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
class AutoDepositBulkTopologyNormalizerTest {
    private final AutoDepositBulkTopologyNormalizer normalizer =
            new AutoDepositBulkTopologyNormalizer();

    @Test
    void normalizesSingleContainersAndOneReciprocalDoubleChest() {
        BlockPos left = new BlockPos(1, 64, 0);
        BlockPos right = new BlockPos(2, 64, 0);
        List<AutoDepositBulkBlockObservation> observations = List.of(
                nonChest(new BlockPos(-2, 64, 0), AutoDepositBulkContainerKind.BARREL, true),
                chest(left, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.LEFT, Direction.NORTH, right, true),
                chest(right, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.RIGHT, Direction.NORTH, left, true),
                chest(new BlockPos(4, 64, 0), AutoDepositBulkContainerKind.TRAPPED_CHEST,
                        AutoDepositBulkChestPart.SINGLE, Direction.SOUTH, null, true)
        );

        AutoDepositBulkTopologyResult result = normalizer.normalize(scan(observations, 4));

        assertTrue(result.success());
        assertEquals(3, result.logicalDestinations().size());
        assertEquals(1, result.doubleChestCollapsedHalfCount());
        AutoDepositBulkLogicalDestination pair = result.logicalDestinations().stream()
                .filter(AutoDepositBulkLogicalDestination::doubleChest)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(left, right), pair.memberPositions());
        assertEquals(left, pair.canonicalFirst());
    }

    @Test
    void acceptsAReciprocalPartnerOutsideTheRequestedVolume() {
        BlockPos inside = new BlockPos(7, 64, 0);
        BlockPos outside = new BlockPos(8, 64, 0);
        List<AutoDepositBulkBlockObservation> observations = List.of(
                chest(inside, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.LEFT, Direction.NORTH, outside, true),
                chest(outside, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.RIGHT, Direction.NORTH, inside, false)
        );

        AutoDepositBulkTopologyResult result = normalizer.normalize(scan(observations, 1));

        assertTrue(result.success());
        assertEquals(1, result.logicalDestinations().size());
        assertEquals(List.of(inside, outside),
                result.logicalDestinations().get(0).memberPositions());
        assertEquals(0, result.doubleChestCollapsedHalfCount());
    }

    @Test
    void keepsTheCanonicalRepresentativeWhenItIsOutsideTheLowVolumeEdge() {
        BlockPos outside = new BlockPos(-9, 64, 0);
        BlockPos inside = new BlockPos(-8, 64, 0);
        List<AutoDepositBulkBlockObservation> observations = List.of(
                chest(inside, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.RIGHT, Direction.NORTH, outside, true),
                chest(outside, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.LEFT, Direction.NORTH, inside, false)
        );

        AutoDepositBulkTopologyResult result = normalizer.normalize(scan(observations, 1));

        assertTrue(result.success());
        AutoDepositBulkLogicalDestination destination =
                result.logicalDestinations().get(0);
        assertEquals(List.of(outside, inside), destination.memberPositions());
        assertEquals(outside, destination.canonicalFirst());
        assertEquals(0, result.doubleChestCollapsedHalfCount());
    }

    @Test
    void pairKeyIsOrderIndependentAndUsesXThenYThenZOrdering() {
        BlockPos first = new BlockPos(-4, 70, 3);
        BlockPos second = new BlockPos(-4, 70, 2);

        AutoDepositDoubleChestPairKey forward =
                AutoDepositDoubleChestPairKey.of(first, second);
        AutoDepositDoubleChestPairKey reverse =
                AutoDepositDoubleChestPairKey.of(second, first);

        assertEquals(forward, reverse);
        assertEquals(second, forward.first());
        assertEquals(first, forward.second());
    }

    @Test
    void anyNonReciprocalOrMismatchedPairFailsTheWholeTopology() {
        BlockPos first = new BlockPos(1, 64, 0);
        BlockPos second = new BlockPos(2, 64, 0);
        AutoDepositBulkBlockObservation left = chest(
                first,
                AutoDepositBulkContainerKind.CHEST,
                AutoDepositBulkChestPart.LEFT,
                Direction.NORTH,
                second,
                true
        );
        AutoDepositBulkBlockObservation wrongFacing = chest(
                second,
                AutoDepositBulkContainerKind.CHEST,
                AutoDepositBulkChestPart.RIGHT,
                Direction.SOUTH,
                first,
                true
        );

        AutoDepositBulkTopologyResult result = normalizer.normalize(
                scan(List.of(
                        nonChest(new BlockPos(-2, 64, 0),
                                AutoDepositBulkContainerKind.BARREL, true),
                        left,
                        wrongFacing
                ), 3)
        );

        assertEquals(AutoDepositBulkTopologyStatus.AMBIGUOUS_DOUBLE_CHEST,
                result.status());
        assertEquals("double_chest_facing_mismatch", result.reason());
        assertTrue(result.logicalDestinations().isEmpty());
        assertFalse(result.success());
    }

    @Test
    void aMissingPartnerIsNotDowngradedToASingleChest() {
        BlockPos first = new BlockPos(1, 64, 0);
        BlockPos second = new BlockPos(2, 64, 0);

        AutoDepositBulkTopologyResult result = normalizer.normalize(scan(List.of(
                chest(first, AutoDepositBulkContainerKind.CHEST,
                        AutoDepositBulkChestPart.LEFT, Direction.NORTH, second, true)
        ), 1));

        assertEquals(AutoDepositBulkTopologyStatus.AMBIGUOUS_DOUBLE_CHEST,
                result.status());
        assertEquals("double_chest_partner_missing", result.reason());
        assertTrue(result.logicalDestinations().isEmpty());
    }

    private static AutoDepositBulkScanResult scan(
            List<AutoDepositBulkBlockObservation> observations,
            int physicalSupportedCount) {
        BlockPos anchor = new BlockPos(0, 64, 0);
        return AutoDepositBulkScanResult.success(
                anchor,
                new AutoDepositBulkWorldProvenance(
                        "singleplayer:test",
                        Dimension.OVERWORLD,
                        "minecraft:overworld",
                        new Object()
                ),
                AutoDepositBulkScanBounds.around(
                        anchor,
                        new AutoDepositBulkBuildHeight(-64, 320)
                ),
                observations,
                observations.size(),
                physicalSupportedCount
        );
    }

    private static AutoDepositBulkBlockObservation nonChest(
            BlockPos position,
            AutoDepositBulkContainerKind kind,
            boolean discovered) {
        return AutoDepositBulkBlockObservation.nonChest(position, kind, discovered);
    }

    private static AutoDepositBulkBlockObservation chest(
            BlockPos position,
            AutoDepositBulkContainerKind kind,
            AutoDepositBulkChestPart part,
            Direction facing,
            BlockPos partner,
            boolean discovered) {
        return AutoDepositBulkBlockObservation.chest(
                position,
                kind,
                part,
                facing,
                partner,
                discovered
        );
    }
}
