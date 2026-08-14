package lavi.minecraft.integration.carryon;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.carryon.container.CarryOnContainerInteractionMonitor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260730_kpopmodder: Register passive Carry On diagnostics without changing ChatClef task behavior.
public final class CarryOnDiagnosticEntrypoint implements ModInitializer {
    private final CarryOnRuntimeStateObserver observer = new CarryOnRuntimeStateObserver();
    private final CarryOnPostPlaceContainerMonitor postPlaceContainerMonitor = new CarryOnPostPlaceContainerMonitor();
    private final CarryOnContainerInteractionMonitor containerInteractionMonitor = new CarryOnContainerInteractionMonitor();

    @Override
    public void onInitialize() {
        ChatClefDiagnostics.registerPostPlaceContainerInteractionObserver(postPlaceContainerMonitor);
        ChatClefDiagnostics.registerBlockInteractionObserver(containerInteractionMonitor);
        ClientTickEvents.END_CLIENT_TICK.register(observer::onEndClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(containerInteractionMonitor::onEndClientTick);
    }
}
