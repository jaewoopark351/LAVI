package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution;

//20260905_kpopmodder: Own the single active ordinary-command lifecycle execution reference.

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class FabricChatClefActiveExecutionStore {
    private final AtomicReference<FabricChatClefCommandExecution> active;

    public FabricChatClefActiveExecutionStore() {
        this(new AtomicReference<>());
    }

    public FabricChatClefActiveExecutionStore(
            AtomicReference<FabricChatClefCommandExecution> active
    ) {
        this.active = Objects.requireNonNull(active, "active");
    }

    public FabricChatClefCommandExecution current() {
        return active.get();
    }

    public boolean installIfAbsent(FabricChatClefCommandExecution execution) {
        return active.compareAndSet(null, execution);
    }

    public void replace(FabricChatClefCommandExecution execution) {
        active.set(execution);
    }

    public boolean clearIfCurrent(FabricChatClefCommandExecution execution) {
        return active.compareAndSet(execution, null);
    }
}
