package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.ownership;

//20260905_kpopmodder: Project active execution and bound-root ownership without mutating lifecycle state.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;

public final class FabricChatClefCommandBoundRootOwnershipReader {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefBoundRootOwnershipView ownershipView;

    public FabricChatClefCommandBoundRootOwnershipReader(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefBoundRootOwnershipView ownershipView
    ) {
        this.executionStore = executionStore;
        this.ownershipView = ownershipView;
    }

    public boolean hasActiveExecution(FabricChatClefCommandContext context) {
        return ownershipView.hasActiveExecution(executionStore.current(), context);
    }

    public boolean matchesBoundRootTask(FabricChatClefCommandContext context, Task candidateTask) {
        return ownershipView.matchesBoundRootTask(executionStore.current(), context, candidateTask);
    }

    public String boundRootMatchReason(FabricChatClefCommandContext context, Task candidateTask) {
        return ownershipView.boundRootMatchReason(executionStore.current(), context, candidateTask);
    }

    public String boundRootOwnershipForDetach(
            FabricChatClefCommandContext context,
            Task candidateTask
    ) {
        return ownershipView.boundRootOwnershipForDetach(
                executionStore.current(),
                context,
                candidateTask
        );
    }

    public String detachCancelAction(FabricChatClefCommandContext context, Task candidateTask) {
        return ownershipView.detachCancelAction(executionStore.current(), context, candidateTask);
    }
}
