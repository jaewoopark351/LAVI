package lavi.minecraft.integration.carryon.snapshot;

import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.CarryOnOperationType;
import lavi.minecraft.integration.carryon.CarryOnTerminalReason;
import lavi.minecraft.integration.carryon.CarryOnTransition;

//20260731_kpopmodder: Group Carry On operation metadata separately from world/player snapshots.
public record CarryOnOperationSnapshot(long taskInstanceId,
                                       long operationId,
                                       String eventName,
                                       CarryOnOperationType operationType,
                                       CarryOnTransition expectedTransition,
                                       long gameTick,
                                       String threadName,
                                       CarryOnObservation stateBefore,
                                       CarryOnObservation stateAfter,
                                       String clickResult,
                                       int attemptCount,
                                       int elapsedTicks,
                                       CarryOnTerminalReason terminalReason) {
}
