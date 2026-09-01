package lavi.minecraft.diagnostics.interaction.ownership;

import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260901_kpopmodder: Retain one exact Task-to-target interaction claim without choosing behavior.
public record BlockInteractionOwnerToken(
        Task sourceTask,
        BlockPos targetPosition,
        long captureClientTick) {
    public BlockInteractionOwnerToken {
        sourceTask = Objects.requireNonNull(sourceTask, "sourceTask");
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition").toImmutable();
        if (captureClientTick < 0L) {
            throw new IllegalArgumentException("captureClientTick must be non-negative");
        }
    }
}
