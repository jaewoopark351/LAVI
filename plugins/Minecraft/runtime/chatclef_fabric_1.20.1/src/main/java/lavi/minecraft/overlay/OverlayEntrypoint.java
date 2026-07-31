package lavi.minecraft.overlay;

import lavi.minecraft.overlay.command.OverlayCommandRegistrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260731_kpopmodder: Register the LAVI overlay command without changing ChatClef command lists.
public final class OverlayEntrypoint implements ModInitializer {
    private final OverlayCommandRegistrar commandRegistrar = new OverlayCommandRegistrar();

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> commandRegistrar.onEndClientTick());
    }
}
