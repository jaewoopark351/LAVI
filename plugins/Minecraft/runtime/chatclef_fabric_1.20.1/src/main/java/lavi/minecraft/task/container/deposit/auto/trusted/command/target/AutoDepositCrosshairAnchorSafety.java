package lavi.minecraft.task.container.deposit.auto.trusted.command.target;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositCrosshairAnchorSafety {
    private static final double MAX_TARGET_DISTANCE_SQUARED = 64.0;

    public Optional<BlockPos> select(
            Screen currentScreen,
            Vec3d playerPosition,
            HitResult crosshairTarget) {
        if (currentScreen != null && !(currentScreen instanceof ChatScreen)) {
            return Optional.empty();
        }
        if (!(crosshairTarget instanceof BlockHitResult blockHit)
                || crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        BlockPos position = blockHit.getBlockPos();
        Vec3d player = Objects.requireNonNull(playerPosition, "playerPosition");
        if (player.squaredDistanceTo(
                position.getX() + 0.5,
                position.getY() + 0.5,
                position.getZ() + 0.5
        ) > MAX_TARGET_DISTANCE_SQUARED) {
            return Optional.empty();
        }
        return Optional.of(position.toImmutable());
    }
}
