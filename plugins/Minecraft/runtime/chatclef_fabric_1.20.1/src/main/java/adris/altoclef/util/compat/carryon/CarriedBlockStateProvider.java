package adris.altoclef.util.compat.carryon;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

//20260727_kpopmodder: Isolates optional carried-block lookups so task code does not depend on Carry On internals.
public interface CarriedBlockStateProvider {

    boolean isCarryingBlock(PlayerEntity player);

    Optional<BlockState> getCarriedBlockState(PlayerEntity player);

    Optional<Identifier> getCarriedBlockId(PlayerEntity player);

    boolean isCarryingBlock(PlayerEntity player, Block expectedBlock);
}
