package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ScreenOpenEvent;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MinecraftClient.class)
public final class ClientOpenScreenMixin {
    @Inject(
            method = "setScreen",
            at = @At("HEAD")
    )
    private void onScreenOpenBegin(@Nullable Screen screen, CallbackInfo ci) {
        ChatClefDiagnostics.logEvent("SCREEN", "HEAD", "setScreen_begin", null,
                "requestedScreenClass", ChatClefDiagnostics.className(screen));
        EventBus.publish(new ScreenOpenEvent(screen, true));
    }

    @Inject(
            method = "setScreen",
            at = @At("TAIL")
    )
    private void onScreenOpenEnd(@Nullable Screen screen, CallbackInfo ci) {
        ChatClefDiagnostics.logEvent("SCREEN", "TAIL", "setScreen_end", null,
                "requestedScreenClass", ChatClefDiagnostics.className(screen));
        ScreenOpenEvent event = new ScreenOpenEvent(screen, false);
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerGuiDiagnostics.onScreenTailSource(event);
        EventBus.publish(event);
    }
}
