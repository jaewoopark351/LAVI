package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Resolve detach ownership without mutating command or engine state.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;

public final class FabricChatClefConnectionDetachDecisionResolver {
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;

    public FabricChatClefConnectionDetachDecisionResolver(
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator
    ) {
        this.lifecycleCoordinator = lifecycleCoordinator;
    }

    public FabricChatClefConnectionDetachDecision resolve(
            FabricChatClefCommandContext context,
            Task currentTask
    ) {
        return new FabricChatClefConnectionDetachDecision(
                lifecycleCoordinator.boundRootMatchReason(context, currentTask),
                lifecycleCoordinator.boundRootOwnershipForDetach(context, currentTask),
                lifecycleCoordinator.detachCancelAction(context, currentTask),
                lifecycleCoordinator.matchesBoundRootTask(context, currentTask)
        );
    }
}
