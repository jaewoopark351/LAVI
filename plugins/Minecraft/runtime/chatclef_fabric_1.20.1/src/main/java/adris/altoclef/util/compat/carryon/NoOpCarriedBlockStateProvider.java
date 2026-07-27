package adris.altoclef.util.compat.carryon;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

//20260727_kpopmodder: Keeps ChatClef loadable when Carry On is not installed or its internals cannot be read.
public final class NoOpCarriedBlockStateProvider implements CarriedBlockStateProvider {

    @Override
    public boolean isCarryingBlock(PlayerEntity player) {
        return false;
    }

    @Override
    public Optional<BlockState> getCarriedBlockState(PlayerEntity player) {
        return Optional.empty();
    }

    @Override
    public Optional<Identifier> getCarriedBlockId(PlayerEntity player) {
        return Optional.empty();
    }

    @Override
    public boolean isCarryingBlock(PlayerEntity player, Block expectedBlock) {
        return false;
    }
}
