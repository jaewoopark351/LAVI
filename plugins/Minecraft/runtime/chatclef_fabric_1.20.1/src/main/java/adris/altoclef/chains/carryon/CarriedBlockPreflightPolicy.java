package adris.altoclef.chains.carryon;

import adris.altoclef.util.compat.CarryOnCompat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

//20260728_kpopmodder: Added this policy to keep Carry On state decisions separate from the task chain.
public final class CarriedBlockPreflightPolicy {

    public boolean shouldPlaceBeforeUserTask(BlockState carriedState) {
        return carriedState != null && !carriedState.isAir();
    }

    public String describeReason(BlockState carriedState) {
        if (carriedState == null) {
            return "missing-state";
        }
        Block block = carriedState.getBlock();
        if (CarryOnCompat.containsCarryOnSensitiveBlock(block)) {
            return "sensitive-carried-block";
        }
        return "carried-block-blocking-actions";
    }

    public String describeBlock(BlockState carriedState) {
        if (carriedState == null) {
            return "none";
        }
        return describeBlock(carriedState.getBlock());
    }

    public String describeBlock(Block block) {
        if (block == null) {
            return "none";
        }
        return block.getTranslationKey();
    }
}
