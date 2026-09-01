package lavi.minecraft.diagnostics.session.lifecycle;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

//20260831_kpopmodder: Emit one mode-eligible final diagnostic snapshot at clean client teardown.
public final class DiagnosticSessionLifecycleEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(
                client -> ChatClefDiagnostics.emitCleanTeardownFinalSnapshot()
        );
    }
}
