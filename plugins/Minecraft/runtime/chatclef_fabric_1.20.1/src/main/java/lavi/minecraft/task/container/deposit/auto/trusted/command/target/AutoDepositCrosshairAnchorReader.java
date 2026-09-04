package lavi.minecraft.task.container.deposit.auto.trusted.command.target;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Read the current crosshair anchor within the existing screen and distance safety boundary.
public final class AutoDepositCrosshairAnchorReader implements AutoDepositCrosshairAnchorSource {
    private final AutoDepositCrosshairAnchorSafety safety;

    public AutoDepositCrosshairAnchorReader() {
        safety = new AutoDepositCrosshairAnchorSafety();
    }

    @Override
    public Optional<BlockPos> read(AltoClef mod) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return Optional.empty();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return Optional.empty();
        }
        return safety.select(
                client.currentScreen,
                mod.getPlayer().getPos(),
                client.crosshairTarget
        );
    }
}
