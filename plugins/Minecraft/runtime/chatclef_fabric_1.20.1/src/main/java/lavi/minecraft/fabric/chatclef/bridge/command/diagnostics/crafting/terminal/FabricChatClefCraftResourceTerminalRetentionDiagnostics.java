package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import net.minecraft.client.MinecraftClient;

//20260901_kpopmodder: Advance diagnostic retention only; never cancel gameplay or commands.
public final class FabricChatClefCraftResourceTerminalRetentionDiagnostics {
    private FabricChatClefCraftResourceTerminalRetentionDiagnostics() {
    }

    public static void onEndClientTick(MinecraftClient ignoredClient) {
        try {
            IronPickaxeAcquisitionScopeDiagnostics.observeRetention(
                    ChatClefDiagnostics.currentClientTickId(),
                    System.nanoTime()
            );
            FabricChatClefCraftResourceTerminalDiagnostics.observeRetention();
        } catch (RuntimeException | LinkageError ignored) {
            // A diagnostics-only retention observer must not alter the client tick lifecycle.
        }
    }
}
