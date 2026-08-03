package lavi.minecraft.diagnostics;

import lavi.minecraft.diagnostics.formatting.DiagnosticValueFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;

//20260731_kpopmodder: Keep current screen/screen-handler reads separate from diagnostic event emission.
final class DiagnosticScreenState {
    private DiagnosticScreenState() {
    }

    static Object[] currentFields() {
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenHandler handler = null;
        if (client != null) {
            ClientPlayerEntity player = client.player;
            if (player != null) {
                handler = player.currentScreenHandler;
            }
        }
        return new Object[]{
                "currentScreenClass", client == null ? "unavailable" : DiagnosticValueFormatter.className(client.currentScreen),
                "currentScreenHandlerClass", handler == null ? "unavailable" : DiagnosticValueFormatter.className(handler),
                "screenHandlerSyncId", handler == null ? "unavailable" : Integer.toString(handler.syncId)
        };
    }
}
