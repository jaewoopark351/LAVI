package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.lifecycle;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.FabricChatClefIronPickaxeRequirementProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalDiagnostics;

//20260913_kpopmodder: Clear only crafting diagnostic state after an owner registration failure.
final class FabricChatClefCraftResourceDiagnosticCleanup {
    private FabricChatClefCraftResourceDiagnosticCleanup() { }

    static void clearAll() {
        clear(IronPickaxeAcquisitionScopeDiagnostics::clearForModeOff);
        clear(FabricChatClefCraftResourceAssociationScopeDiagnostics::clearForModeOff);
        clear(FabricChatClefCraftResourceTargetScopeDiagnostics::clearForModeOff);
        clear(FabricChatClefCraftResourceContainerProjectionSupport::clearForModeOff);
        clear(FabricChatClefIronPickaxeRequirementProjectionDiagnostics::clearForModeOff);
        clear(FabricChatClefCraftResourceTerminalDiagnostics::clearForModeOff);
    }

    private static void clear(Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | LinkageError ignored) {
            // One broken diagnostic collaborator must not retain unrelated crafting ledgers.
        }
    }
}
