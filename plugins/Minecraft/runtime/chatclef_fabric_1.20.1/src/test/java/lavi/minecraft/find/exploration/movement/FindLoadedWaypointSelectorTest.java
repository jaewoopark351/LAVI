//#if MC == 12001
package lavi.minecraft.find.exploration.movement;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import lavi.minecraft.find.observation.FindObservationPort;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify bounded observable work, loaded scope and novelty without a global/native explore process.
class FindLoadedWaypointSelectorTest {
    final FindObservationPort.Binding admission = new FindObservationPort.Binding(new Object(), new Object(),
            "minecraft:overworld", .5, 64, .5, -64, 320);
    @Test void allSixteenWaypointsAreBoundedAndSupportedSelectionRequiresRealTravel() {
        AtomicInteger reads = new AtomicInteger();
        var selected = FindLoadedWaypointSelector.select(admission, new BlockPos(0,64,0), Set.of(), goal -> {
            reads.incrementAndGet();
            assertTrue(Math.hypot(goal.getX(), goal.getZ()) <= 48);
            assertTrue(Math.hypot(goal.getX(), goal.getZ()) >= 16);
            return true;
        }, () -> true);
        assertEquals(16, selected.candidatesVisited());
        assertEquals(16, reads.get());
        assertEquals(16, selected.safeCandidates());
        assertNotNull(selected.waypoint());
        assertEquals(48, Math.hypot(selected.waypoint().getX(), selected.waypoint().getZ()));
    }
    @Test void unsupportedLoadedScopeEndsWithoutFallbackToUnknownWorldOrAWorldWideClaim() {
        AtomicInteger reads = new AtomicInteger();
        var selected = FindLoadedWaypointSelector.select(admission, new BlockPos(0,64,0), Set.of(), goal -> {
            reads.incrementAndGet(); return false;
        }, () -> true);
        assertNull(selected.waypoint());
        assertEquals("no_supported_loaded_exploration_waypoint", selected.reason());
        assertEquals(16, reads.get());
        assertEquals(0, selected.safeCandidates());
    }
    @Test void expirationBeforeAndInsideSafetyReadCannotAdmitAPrefixWaypoint() {
        AtomicInteger reads = new AtomicInteger();
        assertNull(FindLoadedWaypointSelector.select(admission,new BlockPos(0,64,0),Set.of(), goal -> {
            reads.incrementAndGet(); return true;
        }, () -> false).waypoint());
        assertEquals(0, reads.get());
        var live = new boolean[]{true};
        var selected = FindLoadedWaypointSelector.select(admission,new BlockPos(0,64,0),Set.of(), goal -> {
            reads.incrementAndGet(); live[0] = false; return true;
        }, () -> live[0]);
        assertEquals(1, reads.get());
        assertEquals(1, selected.candidatesVisited());
        assertNull(selected.waypoint());
        assertEquals("elapsed_budget_exhausted", selected.reason());
    }
    @Test void visitedGoalsRemainExcludedAndNoveltyRanksFromTheNewLocalStart() {
        var first = FindLoadedWaypointSelector.select(admission,new BlockPos(0,64,0),Set.of(), goal -> true,() -> true);
        var visited = new HashSet<Long>(); visited.add(first.waypoint().asLong());
        var second = FindLoadedWaypointSelector.select(admission,first.waypoint(),visited,goal -> true,() -> true);
        assertNotEquals(first.waypoint(),second.waypoint());
        assertTrue(first.waypoint().getSquaredDistance(second.waypoint()) >= 16 * 16);
        assertTrue(Math.hypot(second.waypoint().getX()-first.waypoint().getX(),
                second.waypoint().getZ()-first.waypoint().getZ()) <= 48);
    }
    @Test void AdmissionBoundIsCheckedBeforeAnyOutOfScopeLoadedSafetyRead() {
        AtomicInteger reads = new AtomicInteger();
        var selected = FindLoadedWaypointSelector.select(admission,new BlockPos(490,64,0),Set.of(), goal -> {
            double distance = Math.hypot(goal.getX(), goal.getZ());
            assertTrue(distance <= 512);
            reads.incrementAndGet(); return true;
        }, () -> true);
        assertNotNull(selected.waypoint());
        assertTrue(Math.hypot(selected.waypoint().getX(),selected.waypoint().getZ()) <= 512);
        assertTrue(reads.get() < 16);
        assertEquals(16, selected.candidatesVisited());
    }
}
//#endif
