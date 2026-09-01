package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.scope;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionActivation;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

//20260901_kpopmodder: Activate only the exact command-owned iron-pickaxe diagnostic scope.
public final class FabricChatClefIronPickaxeScopeActivationDiagnostics {
    private FabricChatClefIronPickaxeScopeActivationDiagnostics() {
    }

    public static IronPickaxeAcquisitionActivation observeDispatchReturned(
            FabricChatClefCommandExecution execution
    ) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || execution == null) {
            return IronPickaxeAcquisitionScopeDiagnostics.activate(
                    false, "", "", -1L, "", "", "", -1L, "", null,
                    "UNAVAILABLE", ChatClefDiagnostics.currentClientTickId(), System.nanoTime()
            );
        }
        FabricChatClefCommandContext context = execution.context();
        FabricChatClefTaskOwnershipEvidence evidence = execution.taskAfterDispatchEvidence();
        return IronPickaxeAcquisitionScopeDiagnostics.activate(
                execution.rootOwnershipClassification()
                        == FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT,
                execution.normalizedCommand(),
                context.sessionId(),
                context.connectionGeneration(),
                context.requestId(),
                context.correlationId(),
                evidence.userTaskRootAssignmentId(),
                evidence.userTaskRootGeneration(),
                evidence.userTaskRootIdentity(),
                evidence.rootTask(),
                evidence.userTaskRootClass(),
                evidence.capturedClientTick(),
                evidence.capturedAtNanos()
        );
    }
}
