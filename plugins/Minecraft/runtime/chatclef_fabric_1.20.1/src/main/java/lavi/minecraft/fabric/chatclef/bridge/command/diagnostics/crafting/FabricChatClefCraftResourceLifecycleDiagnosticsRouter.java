package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionActivation;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.FabricChatClefIronPickaxeRequirementProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.scope.FabricChatClefIronPickaxeScopeActivationDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalLifecycleObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;

import java.util.Map;

//20260901_kpopmodder: Compose split diagnostic observers without owning command lifecycle.
public final class FabricChatClefCraftResourceLifecycleDiagnosticsRouter {
    private FabricChatClefCraftResourceLifecycleDiagnosticsRouter() {
    }

    public static void observe(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || execution == null) {
            return;
        }
        try {
            observeEligible(event, execution, details == null ? Map.of() : details);
        } catch (RuntimeException | LinkageError error) {
            FabricChatClefCraftResourceAssociationReader.observeProjectionFailure(
                    "COMMAND_LIFECYCLE_PROJECTION_" + String.valueOf(event),
                    error
            );
        }
    }

    private static void observeEligible(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        if ("dispatch_returned".equals(event)) {
            IronPickaxeAcquisitionActivation activation =
                    FabricChatClefIronPickaxeScopeActivationDiagnostics.observeDispatchReturned(execution);
            FabricChatClefIronPickaxeRequirementProjectionDiagnostics.observeActivation(activation);
            FabricChatClefCraftResourceAssociationScopeDiagnostics.observeActivation(activation);
            FabricChatClefCraftResourceTargetScopeDiagnostics.observeActivation(activation);
            FabricChatClefCraftResourceTerminalDiagnostics.observeActivation(
                    activation,
                    execution.context()
            );
        }
        FabricChatClefCraftResourceTerminalLifecycleObserver.observe(
                event,
                execution,
                details
        );
    }
}
