package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Resolve only exact-open or current crosshair trusted container targets.
public final class AutoDepositTrustedTargetResolver {
    private static final double MAX_TARGET_DISTANCE_SQUARED = 64.0;

    private final AutoDepositExactOpenContainerBinding openBinding;

    public AutoDepositTrustedTargetResolver(
            AutoDepositExactOpenContainerBinding openBinding) {
        this.openBinding = Objects.requireNonNull(openBinding, "openBinding");
    }

    public Optional<AutoDepositTrustedTarget> resolve(AltoClef mod) {
        Optional<BlockPos> openPosition = openBinding.currentExactPosition();
        if (openPosition.isPresent()) {
            return Optional.of(new AutoDepositTrustedTarget(
                    openPosition.get(), AutoDepositTrustedTargetSource.EXACT_OPEN_CONTAINER
            ));
        }
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return Optional.empty();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen != null
                && !(client.currentScreen instanceof ChatScreen)) {
            return Optional.empty();
        }
        HitResult hit = client == null ? null : client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        BlockPos position = blockHit.getBlockPos();
        if (mod.getPlayer().getPos().squaredDistanceTo(
                position.getX() + 0.5,
                position.getY() + 0.5,
                position.getZ() + 0.5
        ) > MAX_TARGET_DISTANCE_SQUARED) {
            return Optional.empty();
        }
        if (!AutoDepositTrustedContainerSupport.isSupported(
                mod.getWorld().getBlockState(position).getBlock())) {
            return Optional.empty();
        }
        return Optional.of(new AutoDepositTrustedTarget(
                position, AutoDepositTrustedTargetSource.CROSSHAIR
        ));
    }
}
