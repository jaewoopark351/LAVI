package lavi.minecraft.diagnostics.mining.progress;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

//20260830_kpopmodder: Keep existing reset provenance in movement semantic fingerprints.
class MovementProgressSemanticFingerprintTest {
    @Test
    void resetTransitionChangesFingerprintWithoutChangingCheckResult() {
        BlockPos target = new BlockPos(2, 70, 3);
        String notReset = MovementProgressSemanticFingerprint.create(
                target, "DESTROY_MOVE", 1, true, true, false, "NOT_RESET", "PASS");
        String baritoneReset = MovementProgressSemanticFingerprint.create(
                target, "DESTROY_MOVE", 1, true, true, true, "BARITONE_PATHING", "PASS");

        assertNotEquals(notReset, baritoneReset);
    }
}
