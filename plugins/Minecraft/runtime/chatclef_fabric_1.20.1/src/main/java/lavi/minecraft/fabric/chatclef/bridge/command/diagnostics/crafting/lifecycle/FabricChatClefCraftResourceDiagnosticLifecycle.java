package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.lifecycle;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.mode.DiagnosticStateCleanupLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationResult;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.FabricChatClefIronPickaxeRequirementProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalDiagnostics;

import java.util.function.Supplier;

//20260913_kpopmodder: Keep all six required crafting cleanup observers in one atomic diagnostic owner.
public final class FabricChatClefCraftResourceDiagnosticLifecycle {
    private static final DiagnosticOwnerRegistration OWNER = new DiagnosticOwnerRegistration(
            "fabric_craft_resource", FabricChatClefCraftResourceDiagnosticCleanup::clearAll);
    private static final DiagnosticSessionLifecycleObserver[] OBSERVERS = {
            new DiagnosticStateCleanupLifecycleObserver(IronPickaxeAcquisitionScopeDiagnostics::clearForModeOff),
            new DiagnosticStateCleanupLifecycleObserver(FabricChatClefCraftResourceAssociationScopeDiagnostics::clearForModeOff),
            new DiagnosticStateCleanupLifecycleObserver(FabricChatClefCraftResourceTargetScopeDiagnostics::clearForModeOff),
            new DiagnosticStateCleanupLifecycleObserver(FabricChatClefCraftResourceContainerProjectionSupport::clearForModeOff),
            new DiagnosticStateCleanupLifecycleObserver(FabricChatClefIronPickaxeRequirementProjectionDiagnostics::clearForModeOff),
            new DiagnosticStateCleanupLifecycleObserver(FabricChatClefCraftResourceTerminalDiagnostics::clearForModeOff)
    };

    private FabricChatClefCraftResourceDiagnosticLifecycle() { }

    // This is the same registration path used by the production bridge, without transport/config assembly.
    public static DiagnosticObserverRegistrationResult register() {
        return ChatClefDiagnostics.registerSessionLifecycleOwner(OWNER, OBSERVERS);
    }

    public static boolean isAvailable() { return OWNER.isAvailable(); }

    public static void runIfAvailable(Runnable action) {
        register();
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> OWNER.runIfAvailable(action));
    }

    public static <T> T callIfAvailable(Supplier<T> action, T fallback) {
        register();
        return ChatClefDiagnostics.callIfDiagnosticsEligible(() -> OWNER.callIfAvailable(action, fallback), fallback);
    }
}
