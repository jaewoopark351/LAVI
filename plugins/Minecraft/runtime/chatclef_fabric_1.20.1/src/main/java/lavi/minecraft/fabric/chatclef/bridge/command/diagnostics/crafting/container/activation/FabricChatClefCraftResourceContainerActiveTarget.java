package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;

//20260901_kpopmodder: Bind an installed child identity to its proven attempt sequence.
public record FabricChatClefCraftResourceContainerActiveTarget(
        Task parentTask,
        Task activeChildTask,
        CraftResourceTargetTuple targetTuple,
        long targetAttemptSequence) {
    public FabricChatClefCraftResourceContainerActiveTarget {
        parentTask = Objects.requireNonNull(parentTask, "parentTask");
        activeChildTask = Objects.requireNonNull(activeChildTask, "activeChildTask");
        targetTuple = Objects.requireNonNull(targetTuple, "targetTuple");
        if (targetAttemptSequence <= 0L) {
            throw new IllegalArgumentException("targetAttemptSequence must be positive");
        }
    }
}
