package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class MinecraftAutoDepositBulkWorldView implements AutoDepositBulkWorldView {
    private final AltoClef mod;
    private final AutoDepositWorldKeyReader worldKeyReader;

    public MinecraftAutoDepositBulkWorldView(AltoClef mod) {
        this(mod, new AutoDepositWorldKeyReader());
    }

    MinecraftAutoDepositBulkWorldView(
            AltoClef mod,
            AutoDepositWorldKeyReader worldKeyReader) {
        this.mod = Objects.requireNonNull(mod, "mod");
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
    }

    @Override
    public Optional<AutoDepositBulkWorldProvenance> provenance() {
        ClientWorld world = currentWorld();
        if (world == null) {
            return Optional.empty();
        }
        Optional<String> worldKey = worldKeyReader.read();
        if (worldKey.isEmpty()) {
            return Optional.empty();
        }
        Dimension dimension = WorldHelper.getCurrentDimension();
        return Optional.of(new AutoDepositBulkWorldProvenance(
                worldKey.get(),
                dimension,
                world.getRegistryKey().getValue().toString(),
                world
        ));
    }

    @Override
    public Optional<AutoDepositBulkBuildHeight> buildHeight() {
        ClientWorld world = currentWorld();
        return world == null
                ? Optional.empty()
                : Optional.of(new AutoDepositBulkBuildHeight(
                        world.getBottomY(),
                        world.getTopY()
                ));
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        ClientWorld world = currentWorld();
        return world != null && world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public Optional<AutoDepositBulkBlockObservation> observeLoaded(BlockPos position) {
        BlockPos immutablePosition = Objects.requireNonNull(position, "position").toImmutable();
        ClientWorld world = currentWorld();
        int chunkX = immutablePosition.getX() >> 4;
        int chunkZ = immutablePosition.getZ() >> 4;
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
            return Optional.empty();
        }
        BlockState state = world.getBlockState(immutablePosition);
        AutoDepositBulkContainerKind kind = kindOf(state.getBlock());
        if (!kind.chest()) {
            return Optional.of(AutoDepositBulkBlockObservation.nonChest(
                    immutablePosition,
                    kind,
                    false
            ));
        }

        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
        AutoDepositBulkChestPart chestPart = chestPart(chestType);
        Direction facing = state.get(ChestBlock.FACING);
        BlockPos partnerPosition = chestPart.doubleHalf()
                ? immutablePosition.offset(ChestBlock.getFacing(state))
                : null;
        return Optional.of(AutoDepositBulkBlockObservation.chest(
                immutablePosition,
                kind,
                chestPart,
                facing,
                partnerPosition,
                false
        ));
    }

    private ClientWorld currentWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread()) {
            return null;
        }
        ClientWorld world = mod.getWorld();
        if (world == null
                || client.world != world) {
            return null;
        }
        return world;
    }

    private static AutoDepositBulkContainerKind kindOf(Block block) {
        if (block == Blocks.CHEST) {
            return AutoDepositBulkContainerKind.CHEST;
        }
        if (block == Blocks.TRAPPED_CHEST) {
            return AutoDepositBulkContainerKind.TRAPPED_CHEST;
        }
        if (block == Blocks.BARREL) {
            return AutoDepositBulkContainerKind.BARREL;
        }
        return AutoDepositBulkContainerKind.OTHER;
    }

    private static AutoDepositBulkChestPart chestPart(ChestType chestType) {
        return switch (chestType) {
            case SINGLE -> AutoDepositBulkChestPart.SINGLE;
            case LEFT -> AutoDepositBulkChestPart.LEFT;
            case RIGHT -> AutoDepositBulkChestPart.RIGHT;
        };
    }
}
