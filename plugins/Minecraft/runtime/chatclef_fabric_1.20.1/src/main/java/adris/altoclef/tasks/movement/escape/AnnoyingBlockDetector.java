package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.versionedfields.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.util.math.BlockPos;

//20260728_kpopmodder: Shared local obstacle detector for interaction and wander recovery tasks.
public class AnnoyingBlockDetector {
    private static final Block[] ANNOYING_BLOCKS = new Block[]{
            Blocks.VINE,
            Blocks.NETHER_SPROUTS,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.LADDER,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF,
            Blocks.TALL_GRASS,
            Blocks.SHORT_GRASS,
            Blocks.SWEET_BERRY_BUSH
    };

    public BlockPos findNearbyAnnoyingBlock(AltoClef mod) {
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        if (isAnnoying(mod, playerPos)) return playerPos;
        if (isAnnoying(mod, playerPos.up())) return playerPos.up();

        for (BlockPos check : generateSides(playerPos)) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        for (BlockPos check : generateSides(playerPos.up())) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        return null;
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[]{
                pos.add(1, 0, 0),
                pos.add(-1, 0, 0),
                pos.add(0, 0, 1),
                pos.add(0, 0, -1),
                pos.add(1, 0, -1),
                pos.add(1, 0, 1),
                pos.add(-1, 0, -1),
                pos.add(-1, 0, 1)
        };
    }

    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        for (Block annoyingBlock : ANNOYING_BLOCKS) {
            if (block == annoyingBlock) {
                return true;
            }
        }
        return block instanceof DoorBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof FlowerBlock;
    }
}
