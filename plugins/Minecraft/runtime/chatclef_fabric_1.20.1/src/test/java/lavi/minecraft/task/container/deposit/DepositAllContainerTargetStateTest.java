package lavi.minecraft.task.container.deposit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllContainerTargetStateTest {
    @Test
    void storesAnImmutableTargetAndComparesCoordinatesByValue() {
        DepositAllContainerTargetState state = new DepositAllContainerTargetState();
        BlockPos.Mutable mutable = new BlockPos.Mutable(10, 64, 10);

        assertTrue(state.select(mutable));
        mutable.set(20, 70, 20);

        assertEquals(new BlockPos(10, 64, 10), state.selectedTarget().orElseThrow());
        assertTrue(state.matches(new BlockPos(10, 64, 10)));
        assertFalse(state.select(new BlockPos(10, 64, 10)));
    }

    @Test
    void adoptsOnlyInsideTheAcquisitionRangeAndRetainsThroughTheExtraRange() {
        DepositAllContainerTargetState state = new DepositAllContainerTargetState();
        Vec3d player = new Vec3d(0, 64, 0);

        assertFalse(state.selectIfWithin(new BlockPos(80, 64, 0), player, 50));
        assertTrue(state.selectedTarget().isEmpty());

        assertTrue(state.selectIfWithin(new BlockPos(40, 64, 0), player, 50));
        assertFalse(state.isSelectedWithin(new Vec3d(-20, 64, 0), 50));
        assertTrue(state.isSelectedWithin(new Vec3d(-20, 64, 0), 70));
    }

    @Test
    void clearsTheOwnedTargetExactlyOnce() {
        DepositAllContainerTargetState state = new DepositAllContainerTargetState();
        state.select(new BlockPos(1, 2, 3));

        assertTrue(state.clear());
        assertFalse(state.clear());
        assertTrue(state.selectedTarget().isEmpty());
    }
}
