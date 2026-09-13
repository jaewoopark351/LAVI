package lavi.minecraft.blocks.protection;

import lavi.minecraft.blocks.protection.calculation.ProtectionCalculation;
import lavi.minecraft.blocks.protection.calculation.ProtectionVolumeCursor;
import lavi.minecraft.blocks.protection.state.ProtectionSnapshot;
import lavi.minecraft.blocks.scanner.snapshot.BlockLocationSnapshot;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise finite real coordinate traversal, coherent publication, and uncertainty policy.
class ProtectionCalculationTest {
    @Test void immutableValuesDoNotFollowLaterMutableBlockPositionChanges() {
        BlockPos.Mutable mutable = new BlockPos.Mutable(1, 2, 3);
        Object world = new Object(), player = new Object();
        ProtectionSnapshot snapshot = new ProtectionSnapshot(world, player, "dimension", 1, 2, "READY", Set.of(mutable), Set.of());
        BlockLocationSnapshot source = new BlockLocationSnapshot(world, player, "dimension", 1, List.of(mutable));
        mutable.set(9, 9, 9);
        assertEquals(Set.of(new BlockPos(1, 2, 3)), snapshot.protectedBlocks());
        assertEquals(List.of(new BlockPos(1, 2, 3)), source.positions());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.protectedBlocks().clear());
        assertThrows(UnsupportedOperationException.class, () -> source.positions().clear());
    }
    @Test void inclusivePolicyVisitsExactly33CubedAndPreservesTypeSelection() {
        BlockPos marker = new BlockPos(10, 50, -20);
        ProtectionVolumeCursor cursor = new ProtectionVolumeCursor(marker);
        Set<BlockPos> positions = new HashSet<>();
        while (cursor.hasNext()) positions.add(cursor.next());
        assertEquals(35_937, positions.size());
        assertTrue(positions.contains(marker.add(-16, -16, -16)));
        assertTrue(positions.contains(marker.add(16, 16, 16)));
        assertFalse(positions.contains(marker.add(17, 0, 0)));
        ProtectionCalculation calculation = new ProtectionCalculation();
        calculation.indicators(Set.of(marker));
        BlockPos cobble = marker.add(16, 0, 0), log = marker.add(0, -16, 0), ordinary = marker.add(0, 0, -16);
        while (!calculation.pending().isEmpty()) calculation.advance(p -> p.equals(cobble) || p.equals(log));
        assertEquals(Set.of(cobble, log), calculation.completeBlocks());
        assertFalse(calculation.completeBlocks().contains(ordinary));
    }

    @Test void everyTickReevaluationDoesNotRestartThe4096PositionPartition() {
        ProtectionCalculation calculation = new ProtectionCalculation();
        Set<BlockPos> markers = Set.of(BlockPos.ORIGIN);
        AtomicInteger visited = new AtomicInteger();
        int ticks = 0;
        do {
            calculation.indicators(markers);
            int work = calculation.advance(p -> { visited.incrementAndGet(); return false; });
            assertTrue(work <= 4096);
            ticks++;
        } while (!calculation.pending().isEmpty() && ticks < 20);
        assertEquals(9, ticks);
        assertEquals(35_937, visited.get());
        assertTrue(calculation.pending().isEmpty());
    }

    @Test void changesToAlreadyVisitedBlocksDoNotRestartOrLoseNewProtection() {
        ProtectionCalculation calculation = new ProtectionCalculation();
        BlockPos marker = BlockPos.ORIGIN, changed = marker.add(-16, -16, -16);
        calculation.indicators(Set.of(marker));
        calculation.advance(p -> false);
        calculation.blockChanged(changed, true);
        int ticks = 1;
        while (!calculation.pending().isEmpty() && ticks++ < 12) calculation.advance(p -> false);
        assertTrue(calculation.pending().isEmpty());
        assertTrue(calculation.completeBlocks().contains(changed));
        calculation.blockChanged(changed, false);
        assertFalse(calculation.completeBlocks().contains(changed));
    }

    @Test void incompleteRegionsAreConservativeWhileCompletedOrdinaryBlocksRemainUsable() {
        Object world = new Object(), player = new Object();
        ClientProtectionController controller = new ClientProtectionController();
        var source = new BlockLocationSnapshot(world, player, "dimension", 1, List.of(BlockPos.ORIGIN));
        controller.tick(world, player, "dimension", source, p -> true, p -> false);
        ProtectionSnapshot partial = controller.snapshot();
        assertEquals("PREPARING", partial.status());
        assertTrue(partial.protectedBlocks().isEmpty());
        assertTrue(partial.avoids(BlockPos.ORIGIN));
        assertFalse(partial.avoids(new BlockPos(17, 0, 0)));
        for (int n = 0; n < 8; n++) controller.tick(world, player, "dimension", source, p -> true, p -> false);
        assertEquals("READY", controller.snapshot().status());
        assertFalse(controller.snapshot().avoids(BlockPos.ORIGIN));
        assertEquals("PREPARING", partial.status(), "published values cannot mutate after later ticks");
    }

    @Test void newIndicatorIsImmediatelyPendingAndRemovalDoesNotProtectAllBlocksForever() {
        Object world = new Object(), player = new Object();
        ClientProtectionController controller = new ClientProtectionController();
        controller.tick(world, player, "dimension", new BlockLocationSnapshot(world, player, "dimension", 1, List.of()), p -> true, p -> false);
        assertEquals("READY", controller.snapshot().status());
        controller.indicatorChanged(world, BlockPos.ORIGIN, true);
        assertTrue(controller.snapshot().avoids(new BlockPos(16, 0, 0)));
        assertEquals("PREPARING", controller.snapshot().status());
        controller.tick(world, player, "dimension", new BlockLocationSnapshot(world, player, "dimension", 1, List.of()), p -> true, p -> false);
        assertTrue(controller.snapshot().avoids(new BlockPos(16, 0, 0)), "An older scanner list cannot erase a just-observed bed");
        controller.indicatorChanged(world, BlockPos.ORIGIN, false);
        assertEquals("READY", controller.snapshot().status());
        assertFalse(controller.snapshot().avoids(BlockPos.ORIGIN));
    }

    @Test void failedPreparationAndStaleWorldNeverBecomeAnEmptySuccessfulProtectionSet() {
        Object world = new Object(), player = new Object(), replacement = new Object();
        ClientProtectionController controller = new ClientProtectionController();
        var source = new BlockLocationSnapshot(world, player, "dimension", 1, List.of(BlockPos.ORIGIN));
        controller.tick(world, player, "dimension", source, p -> true, p -> { throw new IllegalStateException("capture failed"); });
        assertEquals("FAILED", controller.snapshot().status());
        assertTrue(controller.snapshot().avoids(new BlockPos(400, 40, 400)));
        controller.chunkChanged(world, 0, 0);
        assertEquals("FAILED", controller.snapshot().status());
        controller.tick(replacement, player, "dimension", source, p -> true, p -> false);
        assertEquals("FAILED", controller.snapshot().status());
        assertSame(replacement, controller.snapshot().world());
        var fresh = new BlockLocationSnapshot(replacement, player, "dimension", 2, List.of());
        controller.tick(replacement, player, "dimension", fresh, p -> true, p -> false);
        assertEquals("READY", controller.snapshot().status());
        assertFalse(controller.snapshot().avoids(BlockPos.ORIGIN));
    }

    @Test void invalidationDuringCalculationCannotPublishOldWorldContentsUnderNewMetadata() {
        Object world = new Object(), player = new Object();
        ClientProtectionController controller = new ClientProtectionController();
        var source = new BlockLocationSnapshot(world, player, "dimension", 1, List.of(BlockPos.ORIGIN));
        AtomicInteger samples = new AtomicInteger();
        controller.tick(world, player, "dimension", source, p -> true, p -> {
            if (samples.incrementAndGet() == 5) controller.invalidate();
            return true;
        });
        assertEquals("UNAVAILABLE", controller.snapshot().status());
        assertNull(controller.snapshot().world());
        assertTrue(controller.snapshot().protectedBlocks().isEmpty());
    }
}
