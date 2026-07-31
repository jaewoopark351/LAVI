package lavi.minecraft.integration.carryon.snapshot;

import adris.altoclef.AltoClef;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.CarryOnOperationType;
import lavi.minecraft.integration.carryon.CarryOnTerminalReason;
import lavi.minecraft.integration.carryon.CarryOnTransition;

//20260731_kpopmodder: Collect operation-local diagnostic fields without reading unrelated UI state.
public final class CarryOnOperationSnapshotCollector {
    private CarryOnOperationSnapshotCollector() {
    }

    public static CarryOnOperationSnapshot collect(AltoClef mod,
                                                   long taskInstanceId,
                                                   long operationId,
                                                   String eventName,
                                                   CarryOnOperationType operationType,
                                                   CarryOnTransition expectedTransition,
                                                   CarryOnObservation stateBefore,
                                                   CarryOnObservation stateAfter,
                                                   String clickResult,
                                                   int attemptCount,
                                                   int elapsedTicks,
                                                   CarryOnTerminalReason terminalReason) {
        return new CarryOnOperationSnapshot(
                taskInstanceId,
                operationId,
                CarryOnSnapshotValues.value(eventName),
                operationType == null ? CarryOnOperationType.OBSERVATION_ONLY : operationType,
                expectedTransition == null ? CarryOnTransition.NONE : expectedTransition,
                gameTick(mod),
                Thread.currentThread().getName(),
                stateBefore,
                stateAfter,
                CarryOnSnapshotValues.value(clickResult),
                attemptCount,
                elapsedTicks,
                terminalReason == null ? CarryOnTerminalReason.UNAVAILABLE : terminalReason
        );
    }

    private static long gameTick(AltoClef mod) {
        if (mod == null || mod.getWorld() == null) {
            return -1;
        }
        return mod.getWorld().getTime();
    }
}
