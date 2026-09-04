package lavi.minecraft.task.container.deposit.auto.trusted.command.target;

import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class AutoDepositCrosshairAnchorSafetyTest {
    private final AutoDepositCrosshairAnchorSafety safety =
            new AutoDepositCrosshairAnchorSafety();

    @Test
    void acceptsOnlyBlockHitsWithinEightBlocksOnNoScreenOrChatScreen() {
        BlockPos target = new BlockPos(7, 0, 0);
        BlockHitResult hit = hit(target);

        assertEquals(target, safety.select(null, Vec3d.ZERO, hit).orElseThrow());
        assertEquals(
                target,
                safety.select(new ChatScreen(""), Vec3d.ZERO, hit).orElseThrow()
        );
        assertTrue(safety.select(
                TestObjects.allocate(TitleScreen.class),
                Vec3d.ZERO,
                hit
        ).isEmpty());
    }

    @Test
    void rejectsMissingNonBlockAndOutOfRangeTargets() {
        assertTrue(safety.select(null, Vec3d.ZERO, null).isEmpty());
        assertTrue(safety.select(
                null,
                Vec3d.ZERO,
                BlockHitResult.createMissed(Vec3d.ZERO, Direction.NORTH, BlockPos.ORIGIN)
        ).isEmpty());
        assertTrue(safety.select(null, Vec3d.ZERO, hit(new BlockPos(8, 0, 0))).isEmpty());
    }

    private static BlockHitResult hit(BlockPos position) {
        return new BlockHitResult(
                Vec3d.ofCenter(position),
                Direction.UP,
                position,
                false
        );
    }
}
