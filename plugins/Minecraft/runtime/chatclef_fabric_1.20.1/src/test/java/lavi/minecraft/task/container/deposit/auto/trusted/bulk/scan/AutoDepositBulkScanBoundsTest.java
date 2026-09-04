package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Verify player-anchor build-height validity before Y-window clipping.
class AutoDepositBulkScanBoundsTest {
    @Test
    void usesExactHalfOpenBoundsAtNegativeCoordinates() {
        AutoDepositBulkScanBounds bounds = AutoDepositBulkScanBounds.around(
                new BlockPos(-17, 64, -1),
                new AutoDepositBulkBuildHeight(-64, 320)
        );

        assertEquals(-25, bounds.requestedMinX());
        assertEquals(-9, bounds.requestedMaxXExclusive());
        assertEquals(-9, bounds.requestedMinZ());
        assertEquals(7, bounds.requestedMaxZExclusive());
        assertEquals(56, bounds.effectiveMinY());
        assertEquals(72, bounds.effectiveMaxYExclusive());
        assertEquals(4096, bounds.effectivePositionCount());
        assertTrue(bounds.containsEffective(new BlockPos(-25, 56, -9)));
        assertTrue(bounds.containsEffective(new BlockPos(-10, 71, 6)));
        assertFalse(bounds.containsEffective(new BlockPos(-9, 71, 6)));
        assertFalse(bounds.containsEffective(new BlockPos(-10, 72, 6)));
        assertFalse(bounds.containsEffective(new BlockPos(-10, 71, 7)));
    }

    @Test
    void clampsOnlyTheYIntervalWithoutRebalancingIt() {
        AutoDepositBulkScanBounds bounds = AutoDepositBulkScanBounds.around(
                new BlockPos(4, -60, 8),
                new AutoDepositBulkBuildHeight(-64, 320)
        );

        assertEquals(-68, bounds.requestedMinY());
        assertEquals(-52, bounds.requestedMaxYExclusive());
        assertEquals(-64, bounds.effectiveMinY());
        assertEquals(-52, bounds.effectiveMaxYExclusive());
        assertEquals(16 * 16 * 12, bounds.effectivePositionCount());
    }

    @Test
    void rejectsAnchorsOutsideBuildHeightEvenWhenTheRequestedWindowOverlapsIt() {
        AutoDepositBulkBuildHeight buildHeight = new AutoDepositBulkBuildHeight(-64, 320);

        assertThrows(
                IllegalArgumentException.class,
                () -> AutoDepositBulkScanBounds.around(
                        new BlockPos(0, -65, 0),
                        buildHeight
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AutoDepositBulkScanBounds.around(
                        new BlockPos(0, 320, 0),
                        buildHeight
                )
        );
    }
}
