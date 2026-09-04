package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ClientTickEvent;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Changed this from player to client, I hope this doesn't break anything.
@Mixin(MinecraftClient.class)
public final class  ClientTickMixin {
    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void clientTick(CallbackInfo ci) {
        ChatClefDiagnostics.onClientTickHead();
        EventBus.publish(new ClientTickEvent());
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerGuiDiagnostics.onClientTickBoundaryPublished(ChatClefDiagnostics.currentClientTickId());
    }

    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void clientTickReturn(CallbackInfo ci) {
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerGuiDiagnostics.onClientTickReturn(ChatClefDiagnostics.currentClientTickId());
    }
}
