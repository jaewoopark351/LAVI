package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.ownership;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;

//20260902_kpopmodder: Isolate read-only bound-root ownership projections from lifecycle sequencing.
public final class FabricChatClefBoundRootOwnershipView {
    public boolean hasActiveExecution(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext context
    ) {
        return matchesContext(execution, context);
    }

    public boolean matchesBoundRootTask(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext context,
            Task candidateTask
    ) {
        return matchesContext(execution, context)
                && execution.matchesBoundRootTask(candidateTask);
    }

    public String boundRootMatchReason(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext context,
            Task candidateTask
    ) {
        if (!matchesContext(execution, context)) {
            return "no_matching_lifecycle_execution";
        }
        return execution.boundRootMatchReason(candidateTask);
    }

    public String boundRootOwnershipForDetach(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext context,
            Task candidateTask
    ) {
        if (!matchesContext(execution, context)) {
            return "no_matching_lifecycle_execution";
        }
        return classificationName(execution) + ":" + execution.boundRootMatchReason(candidateTask);
    }

    public String detachCancelAction(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext context,
            Task candidateTask
    ) {
        if (!matchesContext(execution, context)) {
            return "skip_not_owned";
        }
        if (execution.matchesBoundRootTask(candidateTask)) {
            return "cancel_owned_root";
        }
        if (execution.rootOwnershipClassification()
                == FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT) {
            return "skip_preexisting_root";
        }
        return "skip_not_owned";
    }

    private static boolean matchesContext(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext context
    ) {
        return execution != null && execution.context() == context;
    }

    private static String classificationName(FabricChatClefCommandExecution execution) {
        FabricChatClefRootOwnershipClassification classification = execution.rootOwnershipClassification();
        return classification == null ? "OWNERSHIP_UNKNOWN" : classification.name();
    }
}
