package lavi.minecraft.task.container.home.execution.state.context;

import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only the captured STORE_HOME world context.
public final class StoreHomeContextState {
    private HomeStorageOperationContext current;

    public HomeStorageOperationContext current() {
        return current;
    }

    public void capture(HomeStorageOperationContext context) {
        current = Objects.requireNonNull(context, "context");
    }
}
