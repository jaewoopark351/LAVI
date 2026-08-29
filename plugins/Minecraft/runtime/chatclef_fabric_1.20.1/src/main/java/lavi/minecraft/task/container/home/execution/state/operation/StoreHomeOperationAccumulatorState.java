package lavi.minecraft.task.container.home.execution.state.operation;

import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only the current immutable operation accumulator.
public final class StoreHomeOperationAccumulatorState {
    private StoreHomeOperationProgress current;

    public StoreHomeOperationAccumulatorState(StoreHomeOperationProgress initial) {
        current = Objects.requireNonNull(initial, "initial");
    }

    public StoreHomeOperationProgress current() {
        return current;
    }

    public void replace(StoreHomeOperationProgress replacement) {
        current = Objects.requireNonNull(replacement, "replacement");
    }
}
