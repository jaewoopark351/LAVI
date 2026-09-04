package lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

//20260904_kpopmodder: Added the stateless Minecraft adapter for feet-block-position anchors.
public final class MinecraftAutoDepositPlayerPositionAnchorReader
        implements AutoDepositBulkAnchorReader {
    private final AutoDepositBulkPlayerAnchorFactory factory;

    public MinecraftAutoDepositPlayerPositionAnchorReader() {
        this(new AutoDepositBulkPlayerAnchorFactory());
    }

    MinecraftAutoDepositPlayerPositionAnchorReader(
            AutoDepositBulkPlayerAnchorFactory factory) {
        this.factory = java.util.Objects.requireNonNull(factory, "factory");
    }

    @Override
    public AutoDepositBulkAnchorReadResult read(AltoClef mod) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread()) {
            return AutoDepositBulkAnchorReadResult.unavailable();
        }
        ClientWorld clientWorld = client.world;
        ClientPlayerEntity clientPlayer = client.player;
        ClientWorld modWorld = mod == null ? null : mod.getWorld();
        ClientPlayerEntity modPlayer = mod == null ? null : mod.getPlayer();
        Object playerWorld = clientPlayer == null ? null : clientPlayer.getWorld();
        BlockPos playerPosition = clientPlayer == null ? null : clientPlayer.getBlockPos();
        return factory.create(
                true,
                clientWorld,
                modWorld,
                clientPlayer,
                modPlayer,
                playerWorld,
                playerPosition
        );
    }
}
