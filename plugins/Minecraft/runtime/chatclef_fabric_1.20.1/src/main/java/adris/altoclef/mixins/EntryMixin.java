package adris.altoclef.mixins;

import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.TitleScreenEntryEvent;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class EntryMixin {

    @Unique
    private static boolean _initialized = false;

    //#if MC>12002
    @Inject(at = @At("HEAD"), method = "init()V")
    //#elseif MC >= 12001
    //$$ //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
    //$$ @Inject(at = @At("HEAD"), method = "method_25426()V", remap = false)
    //#else
    //$$ @Inject(at = @At("HEAD"), method = "init()V")
    //#endif
    private void init(CallbackInfo info) {
        if (!_initialized) {
            _initialized = true;
            Debug.logMessage("Global Init");
            EventBus.publish(new TitleScreenEntryEvent());
        }
    }
}

