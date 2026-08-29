package lavi.minecraft.task.container.home.execution.operation;

import lavi.minecraft.task.container.home.execution.StoreHomeResult;

//20260828_kpopmodder: Preserve terminal precedence independently of Task lifecycle effects.
public final class StoreHomeTerminalClassifier {
    public StoreHomeResult classifyExhausted(StoreHomeOperationProgress progress) {
        if (progress.storedItems() > 0) {
            return progress.capacityFailures() > 0
                    ? StoreHomeResult.PARTIAL_TRUSTED_CAPACITY_EXHAUSTED
                    : StoreHomeResult.PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE;
        }
        return progress.capacityFailures() > 0 && progress.unavailableFailures() == 0
                ? StoreHomeResult.NO_TRUSTED_CAPACITY
                : StoreHomeResult.NO_USABLE_TRUSTED_DESTINATION;
    }
}
