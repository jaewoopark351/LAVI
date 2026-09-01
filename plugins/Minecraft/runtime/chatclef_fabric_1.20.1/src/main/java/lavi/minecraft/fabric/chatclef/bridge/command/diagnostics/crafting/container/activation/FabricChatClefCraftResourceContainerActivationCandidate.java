package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;

//20260901_kpopmodder: Retain one exact candidate identity until scheduler reconciliation.
public record FabricChatClefCraftResourceContainerActivationCandidate(
        IronPickaxeAcquisitionScopeKey scopeKey,
        Task parentTask,
        Task candidateTask,
        CraftResourceTargetTuple targetTuple,
        CraftResourceSourceEventName candidateSourceEventName) {
    public FabricChatClefCraftResourceContainerActivationCandidate {
        scopeKey = Objects.requireNonNull(scopeKey, "scopeKey");
        parentTask = Objects.requireNonNull(parentTask, "parentTask");
        candidateTask = Objects.requireNonNull(candidateTask, "candidateTask");
        targetTuple = Objects.requireNonNull(targetTuple, "targetTuple");
        candidateSourceEventName = Objects.requireNonNull(
                candidateSourceEventName,
                "candidateSourceEventName"
        );
    }
}
