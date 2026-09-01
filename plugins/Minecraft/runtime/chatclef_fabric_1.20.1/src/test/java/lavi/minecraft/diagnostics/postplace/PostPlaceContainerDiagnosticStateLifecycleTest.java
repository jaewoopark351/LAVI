package lavi.minecraft.diagnostics.postplace;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

//20260831_kpopmodder: Prove mode invalidation cannot revive a pre-OFF post-place intent.
class PostPlaceContainerDiagnosticStateLifecycleTest {
    @Test
    void clearAllDropsTheActiveIntentBeforeANewObservationEpoch() {
        PostPlaceContainerDiagnosticState state = new PostPlaceContainerDiagnosticState();
        BlockPos target = new BlockPos(12, 64, -7);
        state.begin(91L, "CHEST", target, "minecraft:chest", 100L);

        assertNotNull(state.active(target));

        state.clearAll();

        assertNull(state.active(target));
        assertEquals(0, state.attemptCount(91L));
        assertEquals(-1L, state.elapsedTicks(91L, 120L));
    }

    @Test
    void operationCleanupRemainsEffectiveIndependentOfOutputMode() {
        PostPlaceContainerDiagnosticState state = new PostPlaceContainerDiagnosticState();
        BlockPos target = new BlockPos(1, 2, 3);
        state.begin(7L, "BARREL", target, "minecraft:barrel", 10L);

        state.clear(7L);

        assertNull(state.active(target));
    }
}
