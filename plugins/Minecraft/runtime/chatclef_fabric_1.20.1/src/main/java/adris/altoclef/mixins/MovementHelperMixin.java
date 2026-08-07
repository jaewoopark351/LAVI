package adris.altoclef.mixins;

import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.InfestedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MovementHelper.class)
public interface MovementHelperMixin {

    //#if MC >= 12001
    //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
    @Redirect(
            method = "avoidBreaking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_2680;method_26204()Lnet/minecraft/class_2248;",
                    ordinal = 1
            ),
            remap = false
    )
    //#else
    //$$ @Redirect(
    //$$         method = "avoidBreaking",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;",
    //$$                 ordinal = 0
    //$$         )
    //$$ )
    //#endif
    private static Block allowInfested(BlockState instance) {
        // we are able to handle breaking infested blocks...
        if (instance.getBlock() instanceof InfestedBlock infestedBlock) {
            return infestedBlock.getRegularBlock();
        }

        return instance.getBlock();
    }

}
