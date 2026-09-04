package adris.altoclef.mixins.diagnostics;

import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//20260904_kpopmodder: Observe applied server reconciliation without treating packet absence as failure.
@Mixin(ClientPlayNetworkHandler.class)
public final class ClientScreenHandlerUpdateDiagnosticMixin {
    @Inject(
            method = "onScreenHandlerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/ScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1,
            allow = 1
    )
    private void lavi$afterBoundSlotUpdate(
            ScreenHandlerSlotUpdateS2CPacket packet,
            CallbackInfo ci) {
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerGuiDiagnostics.onServerSlotUpdateApplied(
                currentHandler(),
                packet.getSyncId(),
                packet.getRevision(),
                packet.getSlot(),
                packetStack(packet)
        );
    }

    @Inject(
            method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/ScreenHandler;updateSlotStacks(ILjava/util/List;Lnet/minecraft/item/ItemStack;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1,
            allow = 1
    )
    private void lavi$afterBoundFullInventoryUpdate(
            InventoryS2CPacket packet,
            CallbackInfo ci) {
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerGuiDiagnostics.onServerInventoryUpdateApplied(
                currentHandler(),
                packet.getSyncId(),
                packet.getRevision(),
                packet.getContents().size(),
                packet.getCursorStack()
        );
    }

    private static ScreenHandler currentHandler() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null || client.player == null
                ? null
                : client.player.currentScreenHandler;
    }

    private static ItemStack packetStack(ScreenHandlerSlotUpdateS2CPacket packet) {
        //#if MC > 12001
        return packet.getStack();
        //#else
        //$$ return packet.getItemStack();
        //#endif
    }
}
