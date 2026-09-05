package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit;

//20260905_kpopmodder: Release the exact admitted STOP barrier only after confirmed result delivery.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;

import java.util.function.BooleanSupplier;

public final class FabricChatClefStopControlBarrierRelease {
    private final FabricChatClefStopControlQueue stopQueue;

    public FabricChatClefStopControlBarrierRelease(FabricChatClefStopControlQueue stopQueue) {
        this.stopQueue = stopQueue;
    }

    public BooleanSupplier afterSent(FabricChatClefStopControlContext context) {
        return () -> {
            boolean released = stopQueue.completeAndRelease(context);
            if (!released) {
                context.markQuarantined();
            }
            return released;
        };
    }

    public BooleanSupplier retain() {
        return () -> false;
    }
}
