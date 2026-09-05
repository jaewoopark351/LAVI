package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Drain and requeue bounded connection-detach transport events.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;

import java.util.Optional;

public final class FabricChatClefConnectionDetachEventDrainer {
    private static final int MAX_EVENTS_PER_TICK = 16;

    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefConnectionDetachRetirement retirement;

    public FabricChatClefConnectionDetachEventDrainer(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefConnectionDetachRetirement retirement
    ) {
        this.commandQueue = commandQueue;
        this.taskStateReader = taskStateReader;
        this.retirement = retirement;
    }

    public boolean drain() {
        boolean processed = false;
        for (int index = 0; index < MAX_EVENTS_PER_TICK; index++) {
            Optional<FabricChatClefConnectionDetachedEvent> event = commandQueue.pollConnectionDetached();
            if (event.isEmpty()) {
                return processed;
            }
            processed = true;
            if (handle(event.get(), taskStateReader.currentTaskOrNull())) {
                return true;
            }
        }
        return true;
    }

    public boolean handle(FabricChatClefConnectionDetachedEvent event, Task currentTask) {
        boolean deferred = retirement.retire(event, currentTask);
        if (deferred) {
            commandQueue.enqueueConnectionDetached(event.connectionGeneration(), event.reason());
        }
        return deferred;
    }
}
