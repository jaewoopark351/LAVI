package lavi.minecraft.integration.carryon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260730_kpopmodder: Register passive Carry On diagnostics without changing ChatClef task behavior.
public final class CarryOnDiagnosticEntrypoint implements ModInitializer {
    private final CarryOnRuntimeStateObserver observer = new CarryOnRuntimeStateObserver();

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(observer::onEndClientTick);
    }
}
