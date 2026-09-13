package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldBlockModifiedMixin {

    @Unique
    private static boolean hasBlock(BlockState state, BlockPos pos) {
        return !state.isAir() && state.isSolidBlock(MinecraftClient.getInstance().world, pos);
    }

    @Inject(
            method = "onBlockChanged",
            at = @At("HEAD")
    )
    public void onBlockWasChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        //#if MC == 12001
        //20260913_kpopmodder: Preserve exact protection types when client-side block states change between partition ticks.
        if (MinecraftClient.getInstance().isOnThread() && (Object) this == MinecraftClient.getInstance().world
                && adris.altoclef.AltoClef.getInstance() != null) {
            adris.altoclef.AltoClef.getInstance().onUserProtectionBlockChanged((World) (Object) this, pos, newBlock);
        }
        //#endif
        boolean oldHasBlock = hasBlock(oldBlock, pos);
        boolean newHasBlock = hasBlock(newBlock, pos);
        if (ChatClefDiagnostics.isVerboseEnabled()) {
            ChatClefDiagnostics.logEvent("WORLD_BLOCK", "CHANGED", "world_onBlockChanged", null,
                    "blockPosition", pos,
                    "oldBlockState", oldBlock,
                    "newBlockState", newBlock,
                    "oldHasBlock", oldHasBlock,
                    "newHasBlock", newHasBlock);
        }
        if (!oldHasBlock && newHasBlock) {
            BlockPlaceEvent evt = new BlockPlaceEvent(pos, newBlock);
            EventBus.publish(evt);
        }
    }

}
